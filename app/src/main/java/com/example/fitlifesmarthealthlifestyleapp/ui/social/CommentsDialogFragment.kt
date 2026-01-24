package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Comment
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.example.fitlifesmarthealthlifestyleapp.ui.profile.ProfileViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.UUID

class CommentsDialogFragment : DialogFragment(R.layout.fragment_comments) {

    private lateinit var rvComments: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: CommentsAdapter

    private val db = FirebaseFirestore.getInstance()
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private var postId: String? = null

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

        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarComments).apply {
            setNavigationIcon(R.drawable.ic_arrow_back_ios_new_24)
            setNavigationOnClickListener { dismiss() }
        }

        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()
            if (content.isNotEmpty()) {
                handleSendComment(content)
            }
        }
    }

    private fun initViews(view: View) {
        rvComments = view.findViewById(R.id.rvComments)
        etInput = view.findViewById(R.id.etCommentInput)
        btnSend = view.findViewById(R.id.btnSendComment)
        progressBar = view.findViewById(R.id.progressBarComments)
    }

    private fun setupRecyclerView() {
        // Cập nhật Adapter với 2 callback: Reply và LongClick
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
                // Chỉ cho phép chủ sở hữu comment thao tác
                val currentUser = profileViewModel.user.value
                if (currentUser != null && currentUser.uid == comment.userId) {
                    showEditDeleteMenu(comment)
                }
            }
        )

        rvComments.layoutManager = LinearLayoutManager(context)
        rvComments.adapter = adapter
    }

    // --- XỬ LÝ MENU LONG CLICK ---
    private fun showEditDeleteMenu(comment: Comment) {
        val options = arrayOf("Edit Comment", "Delete Comment")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(comment) // Chọn Edit
                    1 -> showDeleteConfirmation(comment) // Chọn Delete
                }
            }
            .show()
    }

    // --- 1. XÓA COMMENT ---
    private fun showDeleteConfirmation(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                deleteComment(comment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        val batch = db.batch()
        val commentRef = db.collection("posts").document(postId!!)
            .collection("comments").document(comment.id)
        batch.delete(commentRef)

        val postRef = db.collection("posts").document(postId!!)
        batch.update(postRef, "commentCount", FieldValue.increment(-1))

        batch.commit().addOnSuccessListener {
            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to delete: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 2. SỬA COMMENT (CÓ GEMINI CHECK) ---
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
        // Check Gemini trước khi update
        lifecycleScope.launch {
            Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()

            // Dùng hàm GeminiModerator đã tạo ở bước trước (tham số ảnh để null)
            val isSafe = GeminiModerator.isContentSafe(requireContext(), null, newContent)

            if (!isSafe) {
                Toast.makeText(context, "Comment contains prohibited material!", Toast.LENGTH_LONG).show()
                return@launch
            }

            // Nếu an toàn -> Update Firestore
            db.collection("posts").document(postId!!)
                .collection("comments").document(comment.id)
                .update("content", newContent)
                .addOnSuccessListener {
                    Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- 3. GỬI COMMENT MỚI (CÓ GEMINI CHECK) ---
    private fun handleSendComment(content: String) {
        val currentUser = profileViewModel.user.value

        if (currentUser != null) {
            checkAndPostComment(currentUser, content)
        } else {
            progressBar.visibility = View.VISIBLE
            btnSend.isEnabled = false
            profileViewModel.fetchUserProfile()

            var userObserver: Observer<User?>? = null
            userObserver = Observer { user ->
                if (user != null) {
                    userObserver?.let { profileViewModel.user.removeObserver(it) }
                    checkAndPostComment(user, content)
                }
            }
            profileViewModel.user.observe(viewLifecycleOwner, userObserver)
        }
    }

    private fun checkAndPostComment(user: User, content: String) {
        lifecycleScope.launch {
            // Hiệu ứng Loading
            btnSend.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()

            // Gọi Gemini check
            val isSafe = GeminiModerator.isContentSafe(requireContext(), null, content)

            if (!isSafe) {
                // Nếu vi phạm
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Content contains prohibited material!", Toast.LENGTH_LONG).show()
                return@launch
            }

            // Nếu an toàn -> Gửi
            executePostComment(user, content)
        }
    }

    private fun executePostComment(user: User, content: String) {
        btnSend.isEnabled = false
        etInput.setText("")

        val commentId = UUID.randomUUID().toString()
        val newComment = Comment(
            id = commentId,
            postId = postId!!,
            userId = user.uid,
            userName = user.displayName,
            userAvatar = user.photoUrl,
            content = content,
            timestamp = Timestamp.now()
        )

        val batch = db.batch()
        val commentRef = db.collection("posts").document(postId!!)
            .collection("comments").document(commentId)
        batch.set(commentRef, newComment)

        val postRef = db.collection("posts").document(postId!!)
        batch.update(postRef, "commentCount", FieldValue.increment(1))

        batch.commit()
            .addOnSuccessListener {
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                sendNotificationToPostOwner(postId!!, user.uid, user.displayName, user.photoUrl, "commented on your post")
            }
            .addOnFailureListener {
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Các phần giữ nguyên
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

    // Hàm tiện ích đổi dp sang px cho padding dialog
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}