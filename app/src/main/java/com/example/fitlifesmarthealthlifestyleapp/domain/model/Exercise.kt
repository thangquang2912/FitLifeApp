package com.example.fitlifesmarthealthlifestyleapp.domain.model

import java.io.Serializable

data class Exercise(
    val id: String = "",
    val name: String = "",
    val durationSeconds: Int = 0,
    val restSeconds: Int = 0,
    val gifUrl: String = "",
    val order: Int = 0
) : Serializable