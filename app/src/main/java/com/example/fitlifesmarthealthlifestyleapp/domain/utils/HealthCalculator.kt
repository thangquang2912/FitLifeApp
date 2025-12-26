package com.example.fitlifesmarthealthlifestyleapp.domain.utils

import com.example.fitlifesmarthealthlifestyleapp.domain.model.User

object HealthCalculator {
    // Phân loại BMI
    fun classifyBMI(bmi: Float): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 24.9 -> "Normal"
            bmi < 29.9 -> "Overweight"
            else -> "Obese"
        }
    }

    // Tính BMR (Basal Metabolic Rate) theo công thức Mifflin-St Jeor
    fun calculateBMR(user: User): Int {
        val age = user.getAge()
        // Công thức cơ bản
        var bmr = (10 * user.weight) + (6.25 * user.height) - (5 * age)

        // Điều chỉnh theo giới tính
        bmr = if (user.gender.equals("Male", ignoreCase = true)) {
            bmr + 5
        } else {
            bmr - 161
        }

        return bmr.toInt()
    }

    // Tính TDEE (Total Daily Energy Expenditure)
    fun calculateTDEE (user: User): Int {
        val bmr = calculateBMR(user)

        // Điều chỉnh theo mức độ vận động (Activity Level Multiplier)
        val multiplier = when (user.activityLevel) {
            "Sedentary" -> 1.2        // Ít vận động
            "Lightly Active" -> 1.375 // Vận động nhẹ
            "Moderately Active" -> 1.55 // Vận động vừa
            "Very Active" -> 1.725    // Năng động
            else -> 1.2
        }

        return (bmr * multiplier).toInt()
    }
}