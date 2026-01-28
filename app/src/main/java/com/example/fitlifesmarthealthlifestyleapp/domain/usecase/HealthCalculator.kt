package com.example.fitlifesmarthealthlifestyleapp.domain.usecase

import com.example.fitlifesmarthealthlifestyleapp.domain.model.User

fun Float.classifyBMI(): String {
    if (this <= 0) return "Unknown"
    return when {
        this < 18.5 -> "Underweight"
        this < 24.9 -> "Normal"
        this < 29.9 -> "Overweight"
        else -> "Obese"
    }
}

fun User.calculateBMR(): Int {
    // Kiểm tra an toàn: Nếu chưa có thông tin cơ bản thì trả về 0 thay vì crash
    if (this.weight <= 0 || this.height <= 0) return 0
    
    val age = this.age

    // Công thức Mifflin-St Jeor
    // weight (kg), height (cm), age (years)
    var bmr = (10 * this.weight) + (6.25 * this.height) - (5 * age)

    // Điều chỉnh theo giới tính (mặc định Male nếu không có dữ liệu)
    bmr = if (this.gender.equals("Female", ignoreCase = true)) {
        bmr - 161
    } else {
        bmr + 5
    }

    return bmr.toInt().coerceAtLeast(0)
}

fun User.calculateTDEE(): Int {
    val bmr = this.calculateBMR()
    if (bmr <= 0) return 0

    // Điều chỉnh theo mức độ vận động
    val multiplier = when (this.activityLevel) {
        "Sedentary" -> 1.2          // Ít vận động
        "Lightly Active" -> 1.375   // 1-3 ngày/tuần
        "Moderately Active" -> 1.55 // 3-5 ngày/tuần
        "Very Active" -> 1.725      // 6-7 ngày/tuần
        else -> 1.2
    }

    return (bmr * multiplier).toInt()
}
