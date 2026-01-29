package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Context
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object NotificationHelper {
    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(
        context: Context,
        recipientId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        postId: String,
        type: String,
        content: String = ""
    ) {
        if (recipientId == senderId) return

        val message = when (type) {
            "LIKE" -> context.getString(R.string.notify_like_post)
            "LIKE_COMMENT" -> context.getString(
                R.string.notify_like_comment,
                content
            )
            "COMMENT" -> context.getString(
                R.string.notify_comment,
                content
            )
            "SHARE" -> context.getString(R.string.notify_share_post)
            "POST" -> context.getString(R.string.notify_post)
            "MESSAGE" -> context.getString(R.string.notify_message)
            "FOLLOW" -> context.getString(R.string.notify_follow)
            else -> context.getString(R.string.notify_default)
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
            message = message,
            isRead = false,
            timestamp = Timestamp.now()
        )

        db.collection("users").document(recipientId)
            .collection("notifications").document(notifId)
            .set(notification)
    }

    fun sendToAllFollowers(
        context: Context,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        postId: String = "",
        type: String = "POST"
    ) {
        db.collection("users").document(senderId).get().addOnSuccessListener { doc ->
            val followers = doc.get("followers") as? List<String> ?: emptyList()
            for (followerId in followers) {
                sendNotification(
                    context = context,
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