package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class CommunityMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",   // Lưu tên người gửi
    val senderAvatar: String = "", // Lưu avatar người gửi
    val text: String = "",
    val imageUrl: String = "",
    val type: String = "TEXT",     // "TEXT" hoặc "IMAGE"
    val timestamp: Timestamp = Timestamp.now()
)