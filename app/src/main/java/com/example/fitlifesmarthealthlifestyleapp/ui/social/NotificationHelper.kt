package com.example.fitlifesmarthealthlifestyleapp.ui.social

import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object NotificationHelper {
    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(
        recipientId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        postId: String,
        type: String,
        content: String = ""
    ) {
        if (recipientId == senderId) return

        val messageText = when (type) {
            "LIKE" -> "liked your post."
            "LIKE_COMMENT" -> "liked your comment: \"$content\""
            "COMMENT" -> "commented: \"$content\""
            "SHARE" -> "shared your post."
            "POST" -> "posted a new update."
            "MESSAGE" -> "sent a message to community group."
            "FOLLOW" -> "started following you."
            else -> "interacted with you."
        }

        val notifId = UUID.randomUUID().toString()
        val notification = Notification(
            id = notifId,
            recipientId = recipientId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            postId = postId,
            type = type,
            message = messageText,
            isRead = false,
            timestamp = Timestamp.now()
        )

        db.collection("users").document(recipientId)
            .collection("notifications").document(notifId)
            .set(notification)
    }

    fun sendToAllFollowers(senderId: String, senderName: String, senderAvatar: String, postId: String = "", type: String = "POST") {
        db.collection("users").document(senderId).get().addOnSuccessListener { doc ->
            // Lấy danh sách những người đang follow mình ( followers )
            val followers = doc.get("followers") as? List<String> ?: emptyList()
            for (followerId in followers) {
                sendNotification(
                    recipientId = followerId,
                    senderId = senderId,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    postId = postId,
                    type = type
                )
            }
        }
    }
}