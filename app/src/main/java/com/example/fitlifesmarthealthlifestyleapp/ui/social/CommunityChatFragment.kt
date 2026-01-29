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
import com.bumptech.glide.Glide
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
    private lateinit var rvCommunityChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnPickImage: ImageView
    private lateinit var layoutPreview: RelativeLayout
    private lateinit var ivPreview: ImageView
    private lateinit var btnClosePreview: ImageView
    private lateinit var progressBarChat: ProgressBar

    private var selectedImageUri: Uri? = null
    private var myName = ""
    private var myAvatar = ""

    // Tracking message being edited
    private var editingMessage: CommunityMessage? = null
    private var isImageRemovedDuringEdit = false

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivPreview.setImageURI(uri)
            layoutPreview.visibility = View.VISIBLE
            isImageRemovedDuringEdit = false
            updateSendButtonState()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCommunityChat = view.findViewById(R.id.rvCommunityChat)
        edtMessage = view.findViewById(R.id.edtMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnPickImage = view.findViewById(R.id.btnPickImage)
        layoutPreview = view.findViewById(R.id.layoutPreview)
        ivPreview = view.findViewById(R.id.ivPreview)
        btnClosePreview = view.findViewById(R.id.btnClosePreview)
        progressBarChat = view.findViewById(R.id.progressBarChat)

        setupRecyclerView()
        fetchMyInfo()
        listenMessages()

        btnPickImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnClosePreview.setOnClickListener {
            selectedImageUri = null
            isImageRemovedDuringEdit = true // Mark that image was removed
            layoutPreview.visibility = View.GONE
            updateSendButtonState()
        }

        edtMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString().trim()

            // Determine which URI to check for safety
            val uriForCheck = if (selectedImageUri != null) {
                selectedImageUri
            } else if (editingMessage != null && !isImageRemovedDuringEdit && editingMessage?.imageUrl?.isNotEmpty() == true) {
                Uri.parse(editingMessage?.imageUrl)
            } else {
                null
            }

            checkSafetyAndProceed(text, uriForCheck) {
                if (editingMessage != null) {
                    performUpdateMessage(editingMessage!!.id, text)
                } else {
                    performSendMessage(text)
                }
            }
        }

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun checkSafetyAndProceed(text: String, uri: Uri?, onSafe: () -> Unit) {
        Toast.makeText(context, "Checking content safety...", Toast.LENGTH_SHORT).show()
        progressBarChat.visibility = View.VISIBLE
        btnSend.isEnabled = false
        edtMessage.isEnabled = false

        lifecycleScope.launch {
            val isSafe = GeminiModerator.isContentSafe(requireContext(), uri, text)
            if (!isSafe) {
                progressBarChat.visibility = View.GONE
                btnSend.isEnabled = true
                edtMessage.isEnabled = true
                Toast.makeText(context, "Content violates community guidelines!", Toast.LENGTH_LONG).show()
                return@launch
            }
            onSafe()
        }
    }

    private fun performSendMessage(text: String) {
        lifecycleScope.launch {
            try {
                var imageUrl = ""
                // Bước 1: Nếu có chọn ảnh cục bộ, upload lên trước
                if (selectedImageUri != null) {
                    imageUrl = CloudinaryHelper.uploadImage(selectedImageUri!!, "community_chats") ?: ""
                }

                // Bước 2: Tạo Message Object chứa CẢ text và imageUrl
                val msgId = UUID.randomUUID().toString()
                val message = CommunityMessage(
                    id = msgId,
                    senderId = currentUid!!,
                    senderName = myName,
                    senderAvatar = myAvatar,
                    text = text, // Văn bản
                    imageUrl = imageUrl, // Ảnh (nếu có)
                    type = if (imageUrl.isNotEmpty()) "IMAGE" else "TEXT", // Để Adapter biết có ảnh hay không
                    timestamp = Timestamp.now()
                )

                db.collection("community_messages").document(msgId).set(message)
                    .addOnSuccessListener {
                        clearInputs()
                        resetInputState()
                    }
                    .addOnFailureListener {
                        resetInputState()
                        Toast.makeText(context, "Send failed", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                resetInputState()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performUpdateMessage(messageId: String, newText: String) {
        lifecycleScope.launch {
            try {
                var finalImageUrl = editingMessage?.imageUrl ?: ""

                // 1. Nếu chọn ảnh MỚI từ gallery/camera
                if (selectedImageUri != null && !selectedImageUri.toString().startsWith("http")) {
                    finalImageUrl = CloudinaryHelper.uploadImage(selectedImageUri!!, "community_chats") ?: ""
                }
                // 2. Nếu đã nhấn nút xóa ảnh trên Preview
                else if (isImageRemovedDuringEdit) {
                    finalImageUrl = ""
                }

                // Gộp cập nhật cả chữ và ảnh
                val updates = mapOf(
                    "text" to newText,
                    "imageUrl" to finalImageUrl,
                    "type" to if (finalImageUrl.isNotEmpty()) "IMAGE" else "TEXT"
                )

                db.collection("community_messages").document(messageId).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Message updated!", Toast.LENGTH_SHORT).show()
                        editingMessage = null
                        isImageRemovedDuringEdit = false
                        clearInputs()
                        resetInputState()
                    }
            } catch (e: Exception) {
                resetInputState()
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDeleteMenu(msg: CommunityMessage, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Edit Message")
        popup.menu.add("Delete Message")

        popup.setOnMenuItemClickListener {
            when (it.title) {
                "Edit Message" -> {
                    editingMessage = msg
                    isImageRemovedDuringEdit = false
                    edtMessage.setText(msg.text)

                    // Nếu tin nhắn có ảnh, hiện ảnh lên ô Preview để cho phép XÓA hoặc THAY THẾ
                    if (msg.imageUrl.isNotEmpty()) {
                        layoutPreview.visibility = View.VISIBLE
                        Glide.with(requireContext()).load(msg.imageUrl).into(ivPreview)
                        // Giả lập Uri từ URL cũ để AI check vẫn hoạt động
                        selectedImageUri = Uri.parse(msg.imageUrl)
                    } else {
                        layoutPreview.visibility = View.GONE
                        selectedImageUri = null
                    }

                    edtMessage.requestFocus()
                    btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange_primary))
                    Toast.makeText(context, "Editing mode enabled", Toast.LENGTH_SHORT).show()
                }
                "Delete Message" -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to permanent delete this message?")
                        .setPositiveButton("Delete") { _, _ ->
                            db.collection("community_messages").document(msg.id).delete()
                                .addOnSuccessListener { Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show() }
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }
            true
        }
        popup.show()
    }

    private fun clearInputs() {
        edtMessage.setText("")
        selectedImageUri = null
        layoutPreview.visibility = View.GONE
    }

    private fun resetInputState() {
        progressBarChat.visibility = View.GONE
        btnSend.isEnabled = true
        edtMessage.isEnabled = true
        btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray_text))
        updateSendButtonState()
    }

    private fun updateSendButtonState() {
        val hasContent = edtMessage.text.toString().trim().isNotEmpty() || selectedImageUri != null || (editingMessage != null && !isImageRemovedDuringEdit)
        btnSend.isEnabled = hasContent
        btnSend.setColorFilter(ContextCompat.getColor(requireContext(),
            if (hasContent) R.color.orange_primary else R.color.gray_text))
    }

    private fun fetchMyInfo() {
        if (currentUid == null) return
        db.collection("users").document(currentUid).get().addOnSuccessListener { doc ->
            myName = doc.getString("displayName") ?: "User"
            myAvatar = doc.getString("photoUrl") ?: ""
        }
    }

    private fun listenMessages() {
        db.collection("community_messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.toObjects(CommunityMessage::class.java) ?: emptyList()
                adapter.updateList(messages)
                if (messages.isNotEmpty()) rvCommunityChat.smoothScrollToPosition(messages.size - 1)
            }
    }

    private fun setupRecyclerView() {
        adapter = CommunityChatAdapter(
            messages = emptyList(),
            onMessageLongClick = { msg, view ->
                if (msg.senderId == currentUid) showEditDeleteMenu(msg, view)
            },
            onImageClick = { url -> FullScreenImageDialogFragment.show(parentFragmentManager, url) },
            onUserClick = { uid ->
                val fragment = if (uid == currentUid) PersonalProfileFragment() else UserProfileFragment.newInstance(uid)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.navHostFragmentContainerView, fragment)
                    .addToBackStack(null).commit()
            }
        )

        rvCommunityChat.layoutManager = LinearLayoutManager(context)
        rvCommunityChat.adapter = adapter
    }
}