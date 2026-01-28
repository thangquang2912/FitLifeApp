package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Comment
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.example.fitlifesmarthealthlifestyleapp.ui.profile.ProfileViewModel
import com.example.fitlifesmarthealthlifestyleapp.utils.NetworkUtils // Đảm bảo đã tạo file này
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.UUID

class CommentsDialogFragment : DialogFragment(R.layout.fragment_comments) {

    // Views
    private lateinit var rvComments: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnAddMedia: ImageView
    private lateinit var progressBar: ProgressBar

    // Preview Media Views (Cần thêm vào XML nếu chưa có)
    private lateinit var layoutPreview: RelativeLayout
    private lateinit var ivPreview: ImageView
    private lateinit var btnRemovePreview: ImageView

    private lateinit var adapter: CommentsAdapter

    // Data & Helpers
    private val db = FirebaseFirestore.getInstance()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private var postId: String? = null

    // Media Selection
    private var selectedMediaUri: Uri? = null
    private var selectedMediaType: String? = null // "IMAGE" hoặc "VIDEO"

    // Launcher chọn ảnh/video
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri

            // Xác định loại file đơn giản
            val mimeType = requireContext().contentResolver.getType(uri)
            selectedMediaType = if (mimeType?.startsWith("video") == true) "VIDEO" else "IMAGE"

            showPreview(uri)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.90).toInt()
            dialog.window?.setLayout(width, height)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString("postId")
        if (postId == null) {
            dismiss()
            return
        }

        initViews(view)
        setupRecyclerView()
        listenToComments()

        if (profileViewModel.user.value == null) {
            profileViewModel.fetchUserProfile()
        }

        // Setup Toolbar
        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarComments).apply {
            setNavigationIcon(R.drawable.ic_arrow_back_ios_new_24)
            setNavigationOnClickListener { dismiss() }
        }

        // Sự kiện nút Thêm Ảnh/Video
        btnAddMedia.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
        }

        // Sự kiện nút Xóa Preview
        btnRemovePreview.setOnClickListener {
            clearPreview()
        }

        // Sự kiện nút Gửi
        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()

            // 1. Kiểm tra mạng
            if (!NetworkUtils.checkConnection(requireContext())) return@setOnClickListener

            // 2. Kiểm tra dữ liệu (Phải có chữ HOẶC có ảnh)
            if (content.isNotEmpty() || selectedMediaUri != null) {
                handleSendComment(content)
            } else {
                Toast.makeText(context, "Please write a comment or select media", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews(view: View) {
        rvComments = view.findViewById(R.id.rvComments)
        etInput = view.findViewById(R.id.etCommentInput)
        btnSend = view.findViewById(R.id.btnSendComment)
        btnAddMedia = view.findViewById(R.id.btnAddMedia) // Nút kẹp giấy/ảnh
        progressBar = view.findViewById(R.id.progressBarComments)

        // Các view cho Preview (Nếu XML chưa có, bạn cần thêm vào layout fragment_comments.xml)
        layoutPreview = view.findViewById(R.id.layoutMediaPreview)
        ivPreview = view.findViewById(R.id.ivMediaPreview)
        btnRemovePreview = view.findViewById(R.id.btnRemoveMedia)
    }

    private fun showPreview(uri: Uri) {
        layoutPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(ivPreview)
    }

    private fun clearPreview() {
        layoutPreview.visibility = View.GONE
        selectedMediaUri = null
        selectedMediaType = null
    }

    private fun setupRecyclerView() {
        adapter = CommentsAdapter(
            onReplyClick = { commentToReply ->
                val replyText = "@${commentToReply.userName} "
                etInput.setText(replyText)
                etInput.setSelection(replyText.length)
                etInput.requestFocus()
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
            },
            onCommentLongClick = { comment ->
                // Chỉ chủ sở hữu mới được sửa/xóa
                val currentUser = profileViewModel.user.value
                if (currentUser != null && currentUser.uid == comment.userId) {
                    // Kiểm tra mạng trước khi hiện menu
                    if (NetworkUtils.checkConnection(requireContext())) {
                        showEditDeleteMenu(comment)
                    }
                }
            }
        )

        rvComments.layoutManager = LinearLayoutManager(context)
        rvComments.adapter = adapter
    }

    // --- LOGIC GỬI COMMENT (QUAN TRỌNG) ---
    private fun handleSendComment(content: String) {
        val currentUser = profileViewModel.user.value

        if (currentUser != null) {
            processSend(currentUser, content)
        } else {
            // Load user nếu chưa có
            progressBar.visibility = View.VISIBLE
            btnSend.isEnabled = false
            profileViewModel.fetchUserProfile()

            var userObserver: Observer<User?>? = null
            userObserver = Observer { user ->
                if (user != null) {
                    userObserver?.let { profileViewModel.user.removeObserver(it) }
                    processSend(user, content)
                }
            }
            profileViewModel.user.observe(viewLifecycleOwner, userObserver)
        }
    }

    private fun processSend(user: User, content: String) {
        lifecycleScope.launch {
            btnSend.isEnabled = false
            progressBar.visibility = View.VISIBLE

            // [MỚI] Xử lý Caption: Nếu rỗng và có chọn ảnh/video -> gán bằng dấu cách " "
            // Nếu không có ảnh mà caption rỗng -> giữ nguyên (để chặn hoặc báo lỗi sau)
            val finalContent = if (content.trim().isEmpty() && selectedMediaUri != null) " " else content

            // 1. Kiểm tra nội dung Text với Gemini
            // Chỉ kiểm tra nếu nội dung có chữ thực sự (khác rỗng và khác dấu cách)
            if (finalContent.trim().isNotEmpty()) {
                val isSafe = GeminiModerator.isContentSafe(requireContext(), null, finalContent)
                if (!isSafe) {
                    btnSend.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Bình luận chứa nội dung không phù hợp!", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            // 2. Upload Media lên Cloudinary (Nếu có)
            var uploadedMediaUrl: String? = null
            if (selectedMediaUri != null) {

                uploadedMediaUrl = CloudinaryHelper.uploadImage(selectedMediaUri!!, "comments")

                if (uploadedMediaUrl == null) {
                    btnSend.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Lỗi upload media. Vui lòng kiểm tra mạng.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            // 3. Lưu vào Firestore (Dùng finalContent)
            executePostComment(user, finalContent, uploadedMediaUrl, selectedMediaType)
        }
    }

    private fun executePostComment(user: User, content: String, mediaUrl: String?, mediaType: String?) {
        val commentId = UUID.randomUUID().toString()

        val newComment = Comment(
            id = commentId,
            postId = postId!!,
            userId = user.uid,
            userName = user.displayName,
            userAvatar = user.photoUrl,
            content = content,
            timestamp = Timestamp.now(),
            mediaUrl = mediaUrl,     // [MỚI]
            mediaType = mediaType    // [MỚI]
        )

        val batch = db.batch()
        val commentRef = db.collection("posts").document(postId!!)
            .collection("comments").document(commentId)
        batch.set(commentRef, newComment)

        val postRef = db.collection("posts").document(postId!!)
        batch.update(postRef, "commentCount", FieldValue.increment(1))

        batch.commit()
            .addOnSuccessListener {
                // Reset UI sau khi thành công
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                etInput.setText("")
                clearPreview() // Xóa ảnh đã chọn

                sendNotificationToPostOwner(postId!!, user.uid, user.displayName, user.photoUrl, "commented on your post")
            }
            .addOnFailureListener {
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- MENU EDIT / DELETE ---
    private fun showEditDeleteMenu(comment: Comment) {
        val options = arrayOf("Edit Comment", "Delete Comment")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                // Kiểm tra mạng lại lần nữa cho chắc
                if (!NetworkUtils.checkConnection(requireContext())) return@setItems

                when (which) {
                    0 -> showEditDialog(comment)
                    1 -> showDeleteConfirmation(comment)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ -> deleteComment(comment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        if (!NetworkUtils.checkConnection(requireContext())) return

        val batch = db.batch()
        val commentRef = db.collection("posts").document(postId!!)
            .collection("comments").document(comment.id)
        batch.delete(commentRef)

        val postRef = db.collection("posts").document(postId!!)
        batch.update(postRef, "commentCount", FieldValue.increment(-1))

        batch.commit().addOnSuccessListener {
            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(comment: Comment) {
        val editText = EditText(requireContext())
        editText.setText(comment.content)
        val padding = dpToPx(16)
        editText.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty() && newContent != comment.content) {
                    checkAndUpdateComment(comment, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndUpdateComment(comment: Comment, newContent: String) {
        lifecycleScope.launch {
            if (!NetworkUtils.checkConnection(requireContext())) return@launch

            Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()
            val isSafe = GeminiModerator.isContentSafe(requireContext(), null, newContent)

            if (!isSafe) {
                Toast.makeText(context, "Content prohibited!", Toast.LENGTH_LONG).show()
                return@launch
            }

            db.collection("posts").document(postId!!)
                .collection("comments").document(comment.id)
                .update("content", newContent)
                .addOnSuccessListener {
                    Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- LISTENERS & NOTIFICATIONS ---
    private fun listenToComments() {
        db.collection("posts").document(postId!!).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                adapter.submitList(comments) {
                    if (comments.isNotEmpty()) {
                        rvComments.smoothScrollToPosition(comments.size - 1)
                    }
                }
            }
    }

    private fun sendNotificationToPostOwner(postId: String, senderId: String, senderName: String, senderAvatar: String, message: String) {
        db.collection("posts").document(postId).get().addOnSuccessListener { document ->
            val ownerId = document.getString("userId") ?: return@addOnSuccessListener
            if (ownerId != senderId) {
                val notifId = UUID.randomUUID().toString()
                val notification = Notification(
                    id = notifId,
                    userId = ownerId,
                    senderId = senderId,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    type = "COMMENT",
                    postId = postId,
                    message = "$senderName $message",
                    createdAt = Timestamp.now()
                )
                db.collection("users").document(ownerId).collection("notifications").document(notifId).set(notification)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}