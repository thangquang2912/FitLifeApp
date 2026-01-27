package com.example.fitlifesmarthealthlifestyleapp.domain.model

data class LeaderboardUser(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val totalSteps: Long = 0,
    val totalDistanceKm: Double = 0.0
)