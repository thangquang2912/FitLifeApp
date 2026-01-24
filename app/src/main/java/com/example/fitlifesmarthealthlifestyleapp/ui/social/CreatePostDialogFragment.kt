package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Post
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.example.fitlifesmarthealthlifestyleapp.ui.profile.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostDialogFragment : BottomSheetDialogFragment() {

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private var selectedUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var imgPreview: ImageView
    private lateinit var etCaption: EditText
    private lateinit var btnShare: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvUploadHint: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgPreview = view.findViewById(R.id.imgPreview)
        etCaption = view.findViewById(R.id.etCaption)
        btnShare = view.findViewById(R.id.btnSharePost)
        progressBar = view.findViewById(R.id.progressBarCreatePost)
        tvUploadHint = view.findViewById(R.id.tvUploadHint)

        // --- BỔ SUNG: Cấu hình chỉ cho nhập số cho Duration và Calories ---
        val etDuration = view.findViewById<TextInputEditText>(R.id.etDuration)
        val etCalories = view.findViewById<TextInputEditText>(R.id.etCalories)

        // TYPE_CLASS_NUMBER: Chỉ hiện bàn phím số
        etDuration?.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etCalories?.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        // -----------------------------------------------------------------

        // 1. Sự kiện chọn ảnh
        view.findViewById<View>(R.id.layoutAddPhoto).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 2. Sự kiện xem ảnh Fullscreen
        imgPreview.setOnClickListener {
            if (selectedUri != null) {
                val activity = context as? AppCompatActivity
                if (activity != null) {
                    FullScreenImageDialogFragment.show(activity.supportFragmentManager, selectedUri.toString())
                }
            }
        }

        // 3. Sự kiện đăng bài
        btnShare.setOnClickListener {
            if (selectedUri == null) {
                Toast.makeText(context, "Please select a photo", Toast.LENGTH_SHORT).show()
            } else {
                uploadAndShare()
            }
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageURI(uri)
            tvUploadHint.text = "Photo Selected "
        }
    }

    private fun uploadAndShare() {
        val caption = etCaption.text.toString().trim()

        progressBar.visibility = View.VISIBLE
        btnShare.isEnabled = false

        val currentUser = profileViewModel.user.value

        if (currentUser == null) {
            profileViewModel.fetchUserProfile()
            profileViewModel.user.observe(viewLifecycleOwner) { loadedUser ->
                if (loadedUser != null) {
                    // THAY ĐỔI: Gọi hàm kiểm duyệt trước khi đăng
                    checkContentAndPost(loadedUser, caption)
                    profileViewModel.user.removeObservers(viewLifecycleOwner)
                }
            }
        } else {
            // THAY ĐỔI: Gọi hàm kiểm duyệt trước khi đăng
            checkContentAndPost(currentUser, caption)
        }
    }

    // --- HÀM MỚI: KIỂM DUYỆT NỘI DUNG BẰNG GEMINI ---
    private fun checkContentAndPost(user: com.example.fitlifesmarthealthlifestyleapp.domain.model.User, caption: String) {
        lifecycleScope.launch {
            // 1. Thông báo cho người dùng biết đang kiểm tra
            Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()

            // 2. Gọi GeminiModerator để kiểm tra (Đảm bảo bạn đã tạo file GeminiModerator.kt như hướng dẫn trước)
            val isSafe = GeminiModerator.isContentSafe(requireContext(), selectedUri, caption)

            if (!isSafe) {
                // 3. Nếu KHÔNG an toàn -> Chặn và thông báo bằng tiếng anh
                resetUI()
                Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                return@launch // Dừng lại, không chạy tiếp
            }

            // 4. Nếu An toàn -> Tiếp tục quy trình Upload cũ
            startPostingProcess(user, caption)
        }
    }

    private fun startPostingProcess(user: com.example.fitlifesmarthealthlifestyleapp.domain.model.User, caption: String) {
        lifecycleScope.launch {
            try {
                // Upload 1 ảnh như cũ
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
                                Toast.makeText(context, "Share successful! ", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                        }
                        .addOnFailureListener { e ->
                            resetUI()
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    resetUI()
                    Toast.makeText(context, "Error when uploading image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                resetUI()
                Toast.makeText(context, "Server Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetUI() {
        progressBar.visibility = View.GONE
        btnShare.isEnabled = true
    }
}