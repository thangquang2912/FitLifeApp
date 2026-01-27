package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class Comment(
    var id: String = "",
    var postId: String = "",
    var userId: String = "",
    var userName: String = "",
    var userAvatar: String = "",
    var content: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var likedBy: MutableList<String> = mutableListOf(),
    val mediaUrl: String? = null,
    val mediaType: String? = null
)