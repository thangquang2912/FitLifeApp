package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class WaterLog(
    var id: String = "", // Sẽ là chuỗi ngày "yyyy-MM-dd"
    var userId: String = "",
    var currentIntake: Int = 0,
    var dailyGoal: Int = 2000,

    @ServerTimestamp
    var lastUpdated: Timestamp? = null
) {
    // Hàm để lấy số chai nước đã uống
    fun getGlassCount(): Int {
        if (dailyGoal == 0) return 0
        return currentIntake / 250
    }
}
