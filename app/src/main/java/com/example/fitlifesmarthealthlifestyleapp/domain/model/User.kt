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
    var height: Int = 170, // Đơn vị: cm
    var weight: Float = 60f, // Đơn vị: kg

    // Mức độ vận động: "Sedentary", "Lightly Active", "Moderately Active", "Very Active"
    // Dùng để tính BMR (Feature 16)
    var activityLevel: String = "Sedentary",
): Parcelable {
    // Tính tuổi
    @get:Exclude
    val age: Int
        get() {
            val today = Calendar.getInstance()
            val dob = Calendar.getInstance()
            dob.time = birthday.toDate()

            if (dob.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                dob.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return 0
            }

            var calculatedAge = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                calculatedAge--
            }
            return calculatedAge
        }

    // 2. Tính BMI
    @get:Exclude
    val bmi: Float
        get() {
            if (height <= 0) return 0f
            val heightInMeter = height / 100f
            return weight / (heightInMeter * heightInMeter)
        }
}