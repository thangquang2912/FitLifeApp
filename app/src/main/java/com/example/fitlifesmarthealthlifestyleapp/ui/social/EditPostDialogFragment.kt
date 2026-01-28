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

    // Bộ chọn ảnh mới từ Thư viện
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedNewUri = uri
            // Hiển thị ảnh mới chọn lên giao diện
            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nhận dữ liệu truyền qua arguments
        arguments?.let {
            postId = it.getString("postId")
            currentImageUrl = it.getString("imageUrl")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Sử dụng lại layout dialog_create_post
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. THAY ĐỔI GIAO DIỆN CHO PHÙ HỢP VỚI EDIT
        val tvTitle = view.findViewById<TextView>(R.id.textView2)
        if (tvTitle != null) tvTitle.text = "Edit Post"

        btnUpdate = view.findViewById(R.id.btnSharePost)
        btnUpdate.text = "Update Post"

        // 2. ÁNH XẠ VIEWS
        val etCaption = view.findViewById<EditText>(R.id.etCaption)
        val etDuration = view.findViewById<TextInputEditText>(R.id.etDuration)
        val etCalories = view.findViewById<TextInputEditText>(R.id.etCalories)

        imgPreview = view.findViewById(R.id.imgPreview)
        progressBar = view.findViewById(R.id.progressBarCreatePost)

        // --- SỬA LỖI Ở ĐÂY: Ánh xạ nút Gallery mới ---
        val btnOpenGallery = view.findViewById<View>(R.id.btnOpenGallery)

        // Ẩn nút Camera trong chế độ Edit (để đơn giản hóa)
        val btnOpenCamera = view.findViewById<View>(R.id.btnOpenCamera)
        btnOpenCamera.visibility = View.GONE

        // 3. ĐIỀN DỮ LIỆU CŨ VÀO FORM
        arguments?.let {
            etCaption.setText(it.getString("caption"))
            // Xóa chữ " mins" và " kcal" để chỉ hiện số cho dễ sửa
            etDuration.setText(it.getString("duration")?.replace(" mins", "")?.trim())
            etCalories.setText(it.getString("calories")?.replace(" kcal", "")?.trim())
        }

        // Hiển thị ảnh cũ
        if (currentImageUrl != null) {
            imgPreview.visibility = View.VISIBLE
            Glide.with(this).load(currentImageUrl).centerCrop().into(imgPreview)
        }

        // 4. SỰ KIỆN CLICK CHỌN ẢNH MỚI (Gallery)
        btnOpenGallery.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 5. SỰ KIỆN ZOOM ẢNH
        imgPreview.setOnClickListener {
            val urlToShow = selectedNewUri?.toString() ?: currentImageUrl
            if (urlToShow != null) {
                val activity = context as? AppCompatActivity
                activity?.let {
                    FullScreenImageDialogFragment.show(it.supportFragmentManager, urlToShow)
                }
            }
        }

        // 6. SỰ KIỆN UPDATE (LOGIC CHÍNH)
        btnUpdate.setOnClickListener {
            val newCaption = etCaption.text.toString().trim()
            var newDuration = etDuration.text.toString().trim()
            var newCalories = etCalories.text.toString().trim()

            // Format lại đơn vị
            if (newDuration.isNotEmpty() && !newDuration.contains("mins")) newDuration += " mins"
            if (newCalories.isNotEmpty() && !newCalories.contains("kcal")) newCalories += " kcal"

            // Nếu Duration/Calories rỗng thì gán mặc định
            if (newDuration.isEmpty()) newDuration = "0 mins"
            if (newCalories.isEmpty()) newCalories = "0 kcal"

            if (postId == null) return@setOnClickListener

            progressBar.visibility = View.VISIBLE
            btnUpdate.isEnabled = false

            // BẮT ĐẦU COROUTINE
            lifecycleScope.launch {
                // --- BƯỚC A: KIỂM DUYỆT GEMINI ---
                Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()

                // Check caption (và ảnh mới nếu có)
                val isSafe = GeminiModerator.isContentSafe(requireContext(), selectedNewUri, newCaption)

                if (!isSafe) {
                    progressBar.visibility = View.GONE
                    btnUpdate.isEnabled = true
                    Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // --- BƯỚC B: UPLOAD ẢNH MỚI (NẾU CÓ) ---
                var finalImageUrl = currentImageUrl // Mặc định dùng ảnh cũ

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
                        Toast.makeText(context, "Error upload: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        btnUpdate.isEnabled = true
                        return@launch
                    }
                }

                // --- BƯỚC C: CẬP NHẬT FIRESTORE ---
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