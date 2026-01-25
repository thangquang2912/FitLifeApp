package com.example.fitlifesmarthealthlifestyleapp.domain.model
import com.google.firebase.Timestamp

data class Notification(
    var id: String = "",
    var userId: String = "", // Người nhận thông báo (chủ bài viết)
    var senderId: String = "", // Người gây ra hành động (người like/comment)
    var senderName: String = "",
    var senderAvatar: String = "",
    var type: String = "", // "LIKE" hoặc "COMMENT"
    var postId: String = "",
    var message: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var isRead: Boolean = false
)