package com.example.fitlifesmarthealthlifestyleapp.domain.model

import java.io.Serializable

data class WorkoutProgram(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val difficulty: String = "",
    val category: String = "",
    val durationMins: Int = 0,
    val caloriesBurn: Int = 0,
    val imageUrl: String = ""
) : Serializable