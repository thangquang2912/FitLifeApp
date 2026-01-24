package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class EditPostDialogFragment : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    // Dữ liệu cũ (nhận từ Bundle)
    private var postId: String? = null
    private var currentImageUrl: String? = null

    // Dữ liệu mới (nếu người dùng chọn ảnh khác)
    private var selectedNewUri: Uri? = null

    // Views
    private lateinit var imgPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnUpdate: Button
    private lateinit var tvUploadHint: TextView

    // Bộ chọn ảnh mới
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedNewUri = uri
            // Hiển thị ảnh mới chọn lên giao diện
            Glide.with(this).load(uri).centerCrop().into(imgPreview)
            imgPreview.visibility = View.VISIBLE

            // Cập nhật giao diện
            tvUploadHint.text = "New photo selected"
            tvUploadHint.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            postId = it.getString("postId")
            currentImageUrl = it.getString("imageUrl")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.textView2)
        if (tvTitle != null) tvTitle.text = "Edit Post"

        val etCaption = view.findViewById<EditText>(R.id.etCaption)
        val etDuration = view.findViewById<TextInputEditText>(R.id.etDuration)
        val etCalories = view.findViewById<TextInputEditText>(R.id.etCalories)

        imgPreview = view.findViewById(R.id.imgPreview)
        btnUpdate = view.findViewById(R.id.btnSharePost)
        progressBar = view.findViewById(R.id.progressBarCreatePost)
        val layoutAddPhoto = view.findViewById<View>(R.id.layoutAddPhoto)
        tvUploadHint = view.findViewById(R.id.tvUploadHint)

        // 1. ĐIỀN DỮ LIỆU CŨ
        arguments?.let {
            etCaption.setText(it.getString("caption"))
            etDuration.setText(it.getString("duration")?.replace(" mins", "")?.trim())
            etCalories.setText(it.getString("calories")?.replace(" kcal", "")?.trim())
        }

        if (currentImageUrl != null) {
            imgPreview.visibility = View.VISIBLE
            Glide.with(this).load(currentImageUrl).centerCrop().into(imgPreview)
            tvUploadHint.text = "Tap to change photo"
        }

        btnUpdate.text = "Update Post"

        // 2. SỰ KIỆN CHỌN ẢNH MỚI
        layoutAddPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 3. SỰ KIỆN ZOOM ẢNH
        imgPreview.setOnClickListener {
            val urlToShow = selectedNewUri?.toString() ?: currentImageUrl
            if (urlToShow != null) {
                val activity = context as? AppCompatActivity
                if (activity != null) {
                    FullScreenImageDialogFragment.show(activity.supportFragmentManager, urlToShow)
                }
            }
        }

        // 4. SỰ KIỆN UPDATE (CÓ GEMINI CHECK)
        btnUpdate.setOnClickListener {
            val newCaption = etCaption.text.toString().trim()
            var newDuration = etDuration.text.toString().trim()
            var newCalories = etCalories.text.toString().trim()

            if (newDuration.isNotEmpty() && !newDuration.contains("mins")) newDuration += " mins"
            if (newCalories.isNotEmpty() && !newCalories.contains("kcal")) newCalories += " kcal"

            if (postId == null) return@setOnClickListener

            progressBar.visibility = View.VISIBLE
            btnUpdate.isEnabled = false

            // BẮT ĐẦU COROUTINE ĐỂ CHECK VÀ UPDATE
            lifecycleScope.launch {
                // --- BƯỚC 1: KIỂM DUYỆT GEMINI ---
                Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()

                // Nếu selectedNewUri là null (không đổi ảnh), Gemini sẽ chỉ check Caption.
                // Nếu selectedNewUri có ảnh mới, Gemini sẽ check cả Ảnh mới + Caption.
                val isSafe = GeminiModerator.isContentSafe(requireContext(), selectedNewUri, newCaption)

                if (!isSafe) {
                    // NẾU KHÔNG AN TOÀN -> DỪNG LẠI
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // --- BƯỚC 2: TIẾN HÀNH UPDATE (NẾU AN TOÀN) ---
                var finalImageUrl = currentImageUrl // Mặc định dùng URL cũ

                // Nếu có ảnh mới -> Upload lên Cloudinary
                if (selectedNewUri != null) {
                    try {
                        Toast.makeText(context, "Uploading new image...", Toast.LENGTH_SHORT).show()
                        val uploadedUrl = CloudinaryHelper.uploadImage(selectedNewUri!!, "community_posts_edits")

                        if (uploadedUrl != null) {
                            finalImageUrl = uploadedUrl
                        } else {
                            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                            btnUpdate.isEnabled = true
                            return@launch
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        btnUpdate.isEnabled = true
                        return@launch
                    }
                }

                // Cập nhật Firestore
                val updates = mapOf(
                    "caption" to newCaption,
                    "duration" to newDuration,
                    "calories" to newCalories,
                    "postImageUrl" to finalImageUrl
                )

                db.collection("posts").document(postId!!)
                    .update(updates)
                    .addOnSuccessListener {
                        if (isAdded) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(context, "Post updated successfully!", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded) {
                            progressBar.visibility = View.GONE
                            btnUpdate.isEnabled = true
                            Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}