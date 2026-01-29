package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    private var selectedUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private lateinit var imgPreview: ImageView
    private lateinit var etCaption: EditText
    private lateinit var btnShare: Button
    private lateinit var progressBar: ProgressBar

    // --- LAUNCHERS ---
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) updatePreview(uri)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess && tempCameraUri != null) updatePreview(tempCameraUri!!)
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgPreview = view.findViewById(R.id.imgPreview)
        etCaption = view.findViewById(R.id.etCaption)
        btnShare = view.findViewById(R.id.btnSharePost)
        progressBar = view.findViewById(R.id.progressBarCreatePost)

        view.findViewById<View>(R.id.btnOpenGallery).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        view.findViewById<View>(R.id.btnOpenCamera).setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        btnShare.setOnClickListener {
            if (selectedUri == null) {
                Toast.makeText(context, "Please select or take a photo", Toast.LENGTH_SHORT).show()
            } else {
                uploadAndShare()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun updatePreview(uri: Uri) {
        selectedUri = uri
        imgPreview.visibility = View.VISIBLE
        imgPreview.setImageURI(uri)
    }

    private fun launchCamera() {
        tempCameraUri = createTempPictureUri()
        takePicture.launch(tempCameraUri)
    }

    private fun createTempPictureUri(): Uri {
        val tempFile = File.createTempFile("camera_img_", ".jpg", requireContext().cacheDir).apply { deleteOnExit() }
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", tempFile)
    }

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
            // ĐÚNG LOGIC GEMINI CỦA BẠN
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
                val imageUrl = CloudinaryHelper.uploadImage(selectedUri!!, "community_posts")
                if (imageUrl != null) {
                    val etDuration = view?.findViewById<TextInputEditText>(R.id.etDuration)
                    val etCalories = view?.findViewById<TextInputEditText>(R.id.etCalories)

                    var durationStr = etDuration?.text.toString().trim().ifEmpty { "0" }
                    var caloriesStr = etCalories?.text.toString().trim().ifEmpty { "0" }
                    if (!durationStr.contains("mins")) durationStr += " mins"
                    if (!caloriesStr.contains("kcal")) caloriesStr += " kcal"

                    val postId = UUID.randomUUID().toString()
                    val post = Post(
                        postId = postId,
                        userId = user.uid,
                        userName = user.displayName,
                        userAvatar = user.photoUrl,
                        postImageUrl = imageUrl,
                        caption = caption,
                        createdAt = Timestamp.now(),
                        duration = durationStr,
                        calories = caloriesStr
                    )

                    db.collection("posts").document(postId).set(post).addOnSuccessListener {
                        if (isAdded) {
                            NotificationHelper.sendToAllFollowers(context = requireContext(),user.uid, user.displayName, user.photoUrl, postId)
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) { resetUI() }
        }
    }

    private fun resetUI() {
        progressBar.visibility = View.GONE
        btnShare.isEnabled = true
    }
}