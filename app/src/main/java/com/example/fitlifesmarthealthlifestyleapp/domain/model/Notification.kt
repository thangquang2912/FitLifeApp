package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val recipientId: String = "", // Người nhận thông báo
    val senderId: String = "",    // Người tạo ra hành động (người like, comment...)
    val senderName: String = "",
    val senderAvatar: String = "",
    val postId: String = "",      // ID bài viết liên quan (nếu có)
    val type: String = "",        // LIKE, COMMENT, SHARE, POST, MESSAGE
    val message: String = "",
    val content: String = "",// Nội dung thông báo (VD: "đã thích bài viết của bạn")
    var isRead: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)