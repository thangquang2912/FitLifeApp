package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class WaterLog(
    var id: String = "",
    var date: String = "",
    var currentIntake: Int = 0,    // Số ml nước đã uống trong ngày
    var dailyGoal: Int = 2000,     // Mục tiêu của ngày đó
    var lastUpdated: Timestamp = Timestamp.now()
) {
    // Tính phần trăm hoàn thành
    fun getProgressPercentage(): Int {
        if (dailyGoal == 0) return 0
        return currentIntake / 250
    }
}
