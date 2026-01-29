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
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File

class EditPostDialogFragment : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private var postId: String? = null
    private var currentImageUrl: String? = null
    private var selectedNewUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private lateinit var imgPreview: ImageView
    private lateinit var etCaption: EditText
    private lateinit var btnUpdate: Button
    private lateinit var progressBar: ProgressBar

    // --- LAUNCHERS ---
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { selectedNewUri = uri; imgPreview.setImageURI(uri) }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) { selectedNewUri = tempCameraUri; imgPreview.setImageURI(tempCameraUri) }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launchCamera() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString("postId")
        currentImageUrl = arguments?.getString("imageUrl")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.textView2).text = "Edit Post"
        imgPreview = view.findViewById(R.id.imgPreview)
        etCaption = view.findViewById(R.id.etCaption)
        btnUpdate = view.findViewById(R.id.btnSharePost)
        progressBar = view.findViewById(R.id.progressBarCreatePost)
        btnUpdate.text = "Update Post"

        etCaption.setText(arguments?.getString("caption"))
        Glide.with(this).load(currentImageUrl).into(imgPreview)
        imgPreview.visibility = View.VISIBLE

        view.findViewById<View>(R.id.btnOpenGallery).setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        // FIX NÚT CAMERA
        view.findViewById<View>(R.id.btnOpenCamera).setOnClickListener {
            requestPermission.launch(Manifest.permission.CAMERA)
        }

        btnUpdate.setOnClickListener { checkSafetyAndUpdate() }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun launchCamera() {
        tempCameraUri = createTempUri()
        takePicture.launch(tempCameraUri)
    }

    private fun createTempUri(): Uri {
        val file = File.createTempFile("edit_img_", ".jpg", requireContext().cacheDir).apply { deleteOnExit() }
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
    }

    private fun checkSafetyAndUpdate() {
        val caption = etCaption.text.toString()
        val checkUri = selectedNewUri ?: Uri.parse(currentImageUrl) // Check ảnh mới hoặc ảnh cũ
        Toast.makeText(context, "Checking content safety...", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
        btnUpdate.isEnabled = false

        lifecycleScope.launch {
            // ĐÚNG LOGIC GEMINI CỦA BẠN
            val isSafe = GeminiModerator.isContentSafe(requireContext(), checkUri, caption)
            if (!isSafe) {
                progressBar.visibility = View.GONE
                btnUpdate.isEnabled = true
                Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            performUpdate()
        }
    }

    private fun performUpdate() {
        lifecycleScope.launch {
            try {
                val finalUrl = if (selectedNewUri != null) CloudinaryHelper.uploadImage(selectedNewUri!!, "community_posts") else currentImageUrl
                val updates = mapOf(
                    "caption" to etCaption.text.toString(),
                    "duration" to view?.findViewById<EditText>(R.id.etDuration)?.text.toString(),
                    "calories" to view?.findViewById<EditText>(R.id.etCalories)?.text.toString(),
                    "postImageUrl" to finalUrl
                )
                db.collection("posts").document(postId!!).update(updates).addOnSuccessListener { dismiss() }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnUpdate.isEnabled = true
            }
        }
    }
}