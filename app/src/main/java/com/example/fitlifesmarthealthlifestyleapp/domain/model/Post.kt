package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class Post(
    var postId: String = "",
    var userId: String = "",
    var userName: String = "",
    var userAvatar: String = "",
    val userEmail: String = "",
    var postImageUrl: String = "",
    var caption: String = "",
    var duration: String = "",
    var calories: String = "",
    var likeCount: Int = 0,
    val shareCount: Int = 0,
    var likedBy: MutableList<String> = mutableListOf(), // Lưu danh sách UID của những người đã Like
    var createdAt: Timestamp = Timestamp.now(),
    var commentCount: Int = 0,
)