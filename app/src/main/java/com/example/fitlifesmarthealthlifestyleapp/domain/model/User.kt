package com.example.fitlifesmarthealthlifestyleapp.domain.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date

@Parcelize
data class User(
    var uid: String = "",
    var email: String = "",
    var displayName: String = "",
    var photoUrl: String = "", // Avatar user
    var createdAt: Timestamp = Timestamp.now(),

    // 2. Chỉ số cơ thể
    var gender: String = "Male",
    var birthday: Timestamp = Timestamp.now(),
    var height: Int = 170, // Đơn vị: cm (Mặc định 170 để tránh chia cho 0)
    var weight: Float = 60f, // Đơn vị: kg

    // Mức độ vận động
    var activityLevel: String = "Sedentary",

    var dailyWaterGoal: Int = 0,
    var dailyStepsGoal: Int = 0,
    var dailyActiveCalories: Int = 0,
    var dailyCaloriesConsume: Int = 0,
    var weeklyRunning: Int = 0,
): Parcelable {
    // Tính tuổi - Kiểm tra an toàn
    @get:Exclude
    val age: Int
        get() {
            try {
                val today = Calendar.getInstance()
                val dob = Calendar.getInstance()
                dob.time = birthday.toDate()

                // Nếu ngày sinh là hôm nay (người dùng mới chưa set)
                if (dob.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    dob.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    return 0
                }

                var calculatedAge = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    calculatedAge--
                }
                return calculatedAge
            } catch (e: Exception) {
                return 0
            }
        }

    // Tính BMI - Chống lỗi chia cho 0
    @get:Exclude
    val bmi: Float
        get() {
            if (height <= 0 || weight <= 0f) return 0f
            val heightInMeter = height / 100f
            return weight / (heightInMeter * heightInMeter)
        }
}
