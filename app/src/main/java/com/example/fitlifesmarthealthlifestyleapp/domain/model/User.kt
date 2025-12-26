package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp
import java.util.Date

data class User(
    var uid: String = "",
    var email: String = "",
    var displayName: String = "",
    var photoUrl: String = "", // Avatar user
    var createdAt: Timestamp = Timestamp.now(),

    // 2. Chỉ số cơ thể
    var gender: String = "Male",
    var birthday: Timestamp = Timestamp.now(),
    var height: Int = 170, // Đơn vị: cm
    var weight: Float = 60f, // Đơn vị: kg

    // Mức độ vận động: "Sedentary", "Lightly Active", "Moderately Active", "Very Active"
    // Dùng để tính BMR (Feature 16)
    var activityLevel: String = "Sedentary",

    // 3. Mục tiêu cá nhân
    var dailyStepGoal: Int = 10000,
    var dailyWaterGoal: Int = 2000,      // (ml)
    var dailyCalorieGoal: Int = 2000,    // (kcal)
    var dailySleepGoal: Int = 8          // (giờ)
) {
    // Tính tuổi
    fun getAge(): Int {
        val today = Date()
        val diff = today.time - birthday.toDate().time
        val yearInMillis = 1000L * 60 * 60 * 24 * 365
        return (diff / yearInMillis).toInt()
    }

    // Tính BMI
    fun getBMI(): Float {
        if (height == 0) return 0f
        val heightInMeter = height / 100f
        return weight / (heightInMeter * heightInMeter)
    }
}