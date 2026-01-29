package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.example.fitlifesmarthealthlifestyleapp.ui.profile.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class CreatePostDialogFragment : BottomSheetDialogFragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()

    // Biến lưu Uri ảnh (Dù chọn từ Gallery hay chụp Camera đều gán vào đây)
    private var selectedUri: Uri? = null

    // Biến lưu Uri tạm thời dành riêng cho Camera (trước khi chụp)
    private var tempCameraUri: Uri? = null

    private lateinit var imgPreview: ImageView
    private lateinit var etCaption: EditText
    private lateinit var btnShare: Button
    private lateinit var progressBar: ProgressBar

    // --- CÁC LAUNCHER XỬ LÝ ẢNH ---

    // 1. Launcher Chọn ảnh từ Thư viện
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            updatePreview(uri)
        }
    }

    // 2. Launcher Chụp ảnh từ Camera
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess && tempCameraUri != null) {
            // Chụp thành công -> Hiển thị ảnh tạm vừa chụp lên
            updatePreview(tempCameraUri!!)
        }
    }

    // 3. Launcher Xin quyền Camera
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Đảm bảo tên layout đúng với file XML mới bạn đã sửa (có 2 nút Gallery/Camera)
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ánh xạ View
        imgPreview = view.findViewById(R.id.imgPreview)
        etCaption = view.findViewById(R.id.etCaption)
        btnShare = view.findViewById(R.id.btnSharePost)
        progressBar = view.findViewById(R.id.progressBarCreatePost)

        val btnOpenGallery = view.findViewById<LinearLayout>(R.id.btnOpenGallery)
        val btnOpenCamera = view.findViewById<LinearLayout>(R.id.btnOpenCamera)

        val etDuration = view.findViewById<TextInputEditText>(R.id.etDuration)
        val etCalories = view.findViewById<TextInputEditText>(R.id.etCalories)

        // Cấu hình chỉ nhập số
        etDuration.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etCalories.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        // --- SỰ KIỆN CLICK ---

        // 1. Click nút Gallery
        btnOpenGallery.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 2. Click nút Camera
        btnOpenCamera.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // 3. Click xem ảnh Fullscreen
        imgPreview.setOnClickListener {
            if (selectedUri != null) {
                val activity = context as? AppCompatActivity
                activity?.let {
                    FullScreenImageDialogFragment.show(it.supportFragmentManager, selectedUri.toString())
                }
            }
        }

        // 4. Click Đăng bài
        btnShare.setOnClickListener {
            if (selectedUri == null) {
                Toast.makeText(context, "Please select or take a photo", Toast.LENGTH_SHORT).show()
            } else {
                uploadAndShare()
            }
        }
    }

    // --- HÀM HỖ TRỢ CAMERA & PREVIEW ---

    private fun updatePreview(uri: Uri) {
        selectedUri = uri
        imgPreview.visibility = View.VISIBLE
        imgPreview.setImageURI(uri)
    }

    private fun launchCamera() {
        try {
            // Tạo file tạm và lấy Uri
            tempCameraUri = createTempPictureUri()
            // Mở camera
            takePicture.launch(tempCameraUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTempPictureUri(): Uri {
        val tempFile = File.createTempFile("camera_img_", ".jpg", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit() // Tự xóa khi app đóng
        }

        // Authority phải trùng với AndroidManifest
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            tempFile
        )
    }

    // --- LOGIC UPLOAD & GEMINI (GIỮ NGUYÊN TỪ CODE CŨ) ---

    private fun uploadAndShare() {
        val rawCaption = etCaption.text.toString()
        val caption = if (rawCaption.trim().isEmpty()) " " else rawCaption

        progressBar.visibility = View.VISIBLE
        btnShare.isEnabled = false

        val currentUser = profileViewModel.user.value

        if (currentUser == null) {
            profileViewModel.fetchUserProfile()
            profileViewModel.user.observe(viewLifecycleOwner) { loadedUser ->
                if (loadedUser != null) {
                    checkContentAndPost(loadedUser, caption)
                    profileViewModel.user.removeObservers(viewLifecycleOwner)
                }
            }
        } else {
            checkContentAndPost(currentUser, caption)
        }
    }

    private fun checkContentAndPost(user: com.example.fitlifesmarthealthlifestyleapp.domain.model.User, caption: String) {
        lifecycleScope.launch {
            Toast.makeText(context, "Checking content safety...", Toast.LENGTH_SHORT).show()

            // Gọi Gemini kiểm tra (selectedUri lúc này có thể là ảnh thư viện HOẶC ảnh camera)
            val isSafe = GeminiModerator.isContentSafe(requireContext(), selectedUri, caption)

            if (!isSafe) {
                resetUI()
                Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                return@launch
            }

            startPostingProcess(user, caption)
        }
    }

    private fun startPostingProcess(user: com.example.fitlifesmarthealthlifestyleapp.domain.model.User, caption: String) {
        lifecycleScope.launch {
            try {
                // Upload ảnh lên Cloudinary
                val imageUrl = CloudinaryHelper.uploadImage(selectedUri!!, "community_posts")

                if (imageUrl != null) {
                    val etDuration = view?.findViewById<TextInputEditText>(R.id.etDuration)
                    val etCalories = view?.findViewById<TextInputEditText>(R.id.etCalories)

                    var durationStr = etDuration?.text.toString().trim()
                    var caloriesStr = etCalories?.text.toString().trim()

                    if (durationStr.isNotEmpty() && !durationStr.contains("mins")) durationStr += " mins"
                    if (caloriesStr.isNotEmpty() && !caloriesStr.contains("kcal")) caloriesStr += " kcal"

                    if (durationStr.isEmpty()) durationStr = "0 mins"
                    if (caloriesStr.isEmpty()) caloriesStr = "0 kcal"

                    val post = Post(
                        postId = UUID.randomUUID().toString(),
                        userId = user.uid,
                        userName = user.displayName,
                        userAvatar = user.photoUrl,
                        userEmail = user.email,
                        postImageUrl = imageUrl,
                        caption = caption,
                        createdAt = Timestamp.now(),
                        likeCount = 0,
                        likedBy = mutableListOf(),
                        duration = durationStr,
                        calories = caloriesStr
                    )

                    db.collection("posts").document(post.postId)
                        .set(post)
                        .addOnSuccessListener {
                            if (isAdded) {
                                Toast.makeText(context, "Shared successfully!", Toast.LENGTH_SHORT).show()

                                // [MỚI] Gửi thông báo cho tất cả người theo dõi
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    NotificationHelper.sendToAllFollowers(
                                        senderId = currentUser.uid,
                                        senderName = currentUser.displayName ?: "User",
                                        senderAvatar = currentUser.photoUrl?.toString() ?: "",
                                        postId = post.postId
                                    )
                                }

                                dismiss()
                            }
                        }
                        .addOnFailureListener { e ->
                            resetUI()
                            Toast.makeText(context, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    resetUI()
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                resetUI()
                Toast.makeText(context, "Server Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetUI() {
        progressBar.visibility = View.GONE
        btnShare.isEnabled = true
    }
}