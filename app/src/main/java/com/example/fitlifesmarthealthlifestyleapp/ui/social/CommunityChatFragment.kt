package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.CommunityMessage
import com.example.fitlifesmarthealthlifestyleapp.ui.nutrition.CloudinaryHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.UUID

class CommunityChatFragment : Fragment(R.layout.fragment_community_chat) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var adapter: CommunityChatAdapter
    private lateinit var rvChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutPreview: RelativeLayout
    private lateinit var ivPreview: ImageView

    private var selectedImageUri: Uri? = null
    private var myName: String = "User"
    private var myAvatar: String = ""

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivPreview.setImageURI(uri)
            layoutPreview.visibility = View.VISIBLE
            updateSendButtonState()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnPickImage = view.findViewById<ImageView>(R.id.btnPickImage)
        val btnClosePreview = view.findViewById<ImageView>(R.id.btnClosePreview)
        rvChat = view.findViewById(R.id.rvCommunityChat)
        edtMessage = view.findViewById(R.id.edtMessage)
        btnSend = view.findViewById(R.id.btnSend)
        progressBar = view.findViewById(R.id.progressBarChat)
        layoutPreview = view.findViewById(R.id.layoutPreview)
        ivPreview = view.findViewById(R.id.ivPreview)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        fetchMyInfo()

        // [CẬP NHẬT] Setup Adapter với đầy đủ callback
        adapter = CommunityChatAdapter(
            messages = emptyList(),
            onMessageLongClick = { msg, v -> showMessageOptions(msg, v) },

            // 1. Callback Xem ảnh
            onImageClick = { imageUrl ->
                FullScreenImageDialogFragment.show(parentFragmentManager, imageUrl)
            },

            // 2. Callback Xem Profile
            onUserClick = { userId ->
                openUserProfile(userId)
            }
        )

        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        rvChat.layoutManager = layoutManager
        rvChat.adapter = adapter
        rvChat.setPadding(0, 0, 0, dpToPx(16))

        listenMessages()

        btnPickImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnClosePreview.setOnClickListener {
            selectedImageUri = null
            layoutPreview.visibility = View.GONE
            updateSendButtonState()
        }

        edtMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateSendButtonState()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSend.setOnClickListener { handleSendMessage() }
    }

    // [MỚI] Hàm xử lý điều hướng Profile
    private fun openUserProfile(userId: String) {
        val transaction = parentFragmentManager.beginTransaction()

        if (userId == currentUid) {
            // Nếu click vào chính mình -> Vào Personal Profile
            transaction.replace(R.id.navHostFragmentContainerView, PersonalProfileFragment())
        } else {
            // Nếu click vào người khác -> Vào User Profile (dùng newInstance để truyền ID)
            transaction.replace(R.id.navHostFragmentContainerView, UserProfileFragment.newInstance(userId))
        }

        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showMessageOptions(msg: CommunityMessage, view: View) {
        val popup = PopupMenu(context, view)
        if (msg.senderId == currentUid) {
            popup.menu.add("Edit")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Edit" -> {
                        if (msg.type == "TEXT") {
                            showEditDialog(msg)
                        } else {
                            Toast.makeText(context, "Cannot edit image message", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    "Delete" -> {
                        showDeleteConfirmDialog(msg)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun showEditDialog(msg: CommunityMessage) {
        val input = EditText(context)
        input.setText(msg.text)

        AlertDialog.Builder(context)
            .setTitle("Edit Message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    db.collection("community_messages").document(msg.id)
                        .update("text", newText)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmDialog(msg: CommunityMessage) {
        AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("community_messages").document(msg.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleSendMessage() {
        val text = edtMessage.text.toString().trim()
        val context = requireContext()
        Toast.makeText(context, "Checking content safety...", Toast.LENGTH_SHORT).show()
        btnSend.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        edtMessage.isEnabled = false

        lifecycleScope.launch {
            val isSafe = GeminiModerator.isContentSafe(context, selectedImageUri, text)

            if (!isSafe) {
                Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                resetInputState()
                return@launch
            }

            var uploadedImageUrl = ""
            var messageType = "TEXT"

            if (selectedImageUri != null) {
                val url = CloudinaryHelper.uploadImage(selectedImageUri!!, "community_chat")
                if (url != null) {
                    uploadedImageUrl = url
                    messageType = "IMAGE"
                } else {
                    Toast.makeText(context, "Upload error", Toast.LENGTH_SHORT).show()
                    resetInputState()
                    return@launch
                }
            }

            saveToFirestore(text, uploadedImageUrl, messageType)
        }
    }

    private fun saveToFirestore(text: String, imageUrl: String, type: String) {
        if (currentUid == null) return

        val msgId = UUID.randomUUID().toString()
        val message = CommunityMessage(
            id = msgId,
            senderId = currentUid,
            senderName = myName,
            senderAvatar = myAvatar,
            text = text,
            imageUrl = imageUrl,
            type = type,
            timestamp = Timestamp.now()
        )

        db.collection("community_messages").document(msgId).set(message)
            .addOnSuccessListener {
                // [MỚI] Gửi thông báo đến những người đang follow mình
                NotificationHelper.sendToAllFollowers(
                    senderId = currentUid,
                    senderName = myName,
                    senderAvatar = myAvatar,
                    postId = "", // Tin nhắn chat không gắn với bài viết cụ thể
                    type = "MESSAGE"
                )

                edtMessage.setText("")
                selectedImageUri = null
                layoutPreview.visibility = View.GONE
                resetInputState()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                resetInputState()
            }
    }

    private fun fetchMyInfo() {
        if (currentUid == null) return
        db.collection("users").document(currentUid).get().addOnSuccessListener { doc ->
            myName = doc.getString("displayName") ?: "User"
            myAvatar = doc.getString("photoUrl") ?: ""
        }
    }

    private fun listenMessages() {
        db.collection("community_messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val messages = snapshot?.toObjects(CommunityMessage::class.java) ?: emptyList()
                adapter.updateList(messages)
                if (messages.isNotEmpty()) {
                    rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
    }

    private fun resetInputState() {
        btnSend.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        edtMessage.isEnabled = true
        updateSendButtonState()
    }

    private fun updateSendButtonState() {
        val hasText = edtMessage.text.toString().trim().isNotEmpty()
        val hasImage = selectedImageUri != null
        val enable = hasText || hasImage

        btnSend.isEnabled = enable
        btnSend.setColorFilter(ContextCompat.getColor(requireContext(),
            if (enable) R.color.orange_primary else R.color.gray_text))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}