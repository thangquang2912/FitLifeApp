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
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.example.fitlifesmarthealthlifestyleapp.ui.profile.ProfileViewModel
import com.example.fitlifesmarthealthlifestyleapp.utils.NetworkUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

    // Preview Media Views
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

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
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

        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarComments).apply {
            setNavigationIcon(R.drawable.ic_arrow_back_ios_new_24)
            setNavigationOnClickListener { dismiss() }
        }

        btnAddMedia.setOnClickListener {
            if (NetworkUtils.checkConnection(requireContext())) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
        }

        btnRemovePreview.setOnClickListener {
            clearPreview()
        }

        btnSend.setOnClickListener {
            val content = etInput.text.toString().trim()
            if (!NetworkUtils.checkConnection(requireContext())) return@setOnClickListener

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
        btnAddMedia = view.findViewById(R.id.btnAddMedia)
        progressBar = view.findViewById(R.id.progressBarComments)

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
                val currentUser = profileViewModel.user.value
                if (currentUser != null && currentUser.uid == comment.userId) {
                    if (NetworkUtils.checkConnection(requireContext())) {
                        showEditDeleteMenu(comment)
                    }
                }
            },
            // [MỚI] Xử lý click User Profile
            onUserClick = { userId ->
                // Đóng Dialog bình luận trước
                dismiss()

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                val transaction = requireActivity().supportFragmentManager.beginTransaction()

                if (userId == currentUid) {
                    // Chuyển đến trang cá nhân của mình
                    transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
                } else {
                    // Chuyển đến trang người khác
                    transaction.replace(R.id.navHostFragmentContainerView, UserProfileFragment.newInstance(userId))
                }
                transaction.addToBackStack(null)
                transaction.commit()
            }
        )
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                adapter.setCurrentUserInfo(it.displayName, it.photoUrl ?: "")
            }
        }
        rvComments.layoutManager = LinearLayoutManager(context)
        rvComments.adapter = adapter
    }

    private fun handleSendComment(content: String) {
        val currentUser = profileViewModel.user.value
        if (currentUser != null) {
            processSend(currentUser, content)
        } else {
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
            Toast.makeText(context, "Checking content...", Toast.LENGTH_SHORT).show()
            btnSend.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val finalContent = if (content.trim().isEmpty() && selectedMediaUri != null) " " else content

            if (finalContent.trim().isNotEmpty()) {
                val isSafe = GeminiModerator.isContentSafe(requireContext(), null, finalContent)
                if (!isSafe) {
                    btnSend.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Bình luận chứa nội dung không phù hợp!", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

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
            mediaUrl = mediaUrl,
            mediaType = mediaType
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
                etInput.setText("")
                clearPreview()
                sendNotificationToPostOwner(postId!!, user.uid, user.displayName, user.photoUrl, content)
            }
            .addOnFailureListener {
                btnSend.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDeleteMenu(comment: Comment) {
        val options = arrayOf("Edit Comment", "Delete Comment")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
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

    // Trong CommentsDialogFragment.kt
    private fun sendNotificationToPostOwner(postId: String, senderId: String, senderName: String, senderAvatar: String, message: String) {
        db.collection("posts").document(postId).get().addOnSuccessListener { document ->
            val ownerId = document.getString("userId") ?: return@addOnSuccessListener

            // Gọi Helper để gửi thông báo
            NotificationHelper.sendNotification(
                context = requireContext(),
                recipientId = ownerId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                postId = postId,
                type = "COMMENT",
                content = message
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}