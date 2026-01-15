package com.example.fitlifesmarthealthlifestyleapp.domain.model

import java.util.Date

data class Meal(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val portion: Float = 0f,
    val timestamp: Date = Date(),
    val imageUrl: String? = null
)