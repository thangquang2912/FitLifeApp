package com.example.fitlifesmarthealthlifestyleapp.data.repository

import android.util.Log // 1. Thêm Import này
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Meal
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NutritionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val mealsCollection = db.collection("meals")
    private val summaryCollection = db.collection("daily_nutrition")

    suspend fun addMeal(meal: Meal): Result<Boolean> {
        return try {
            val batch = db.batch()
            val mealRef = mealsCollection.document(meal.id)
            batch.set(mealRef, meal)

            val dateId = getDateId(meal.timestamp)
            val summaryId = "${meal.userId}_$dateId"
            val summaryRef = summaryCollection.document(summaryId)

            val updates = hashMapOf<String, Any>(
                "userId" to meal.userId,
                "dateId" to dateId,
                "date" to meal.timestamp,
                "totalCalories" to FieldValue.increment(meal.calories.toLong()),
                "totalCarbs" to FieldValue.increment(meal.carbs.toDouble()),
                "totalProtein" to FieldValue.increment(meal.protein.toDouble()),
                "totalFat" to FieldValue.increment(meal.fat.toDouble()),
                "mealsCount" to FieldValue.increment(1)
            )

            batch.set(summaryRef, updates, SetOptions.merge())
            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... (Hàm getDailySummary giữ nguyên) ...
    suspend fun getDailySummary(userId: String, date: Date): Map<String, Any>? {
        return try {
            val dateId = getDateId(date)
            val summaryId = "${userId}_$dateId"
            val snapshot = summaryCollection.document(summaryId).get().await()
            snapshot.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMealsByDate(userId: String, date: Date): Result<List<Meal>> {
        return try {
            val start = getStartOfDay(date)
            val end = getEndOfDay(date)

            Log.d("NutritionRepo", "Đang tìm meal của user: $userId từ $start đến $end")

            val snapshot = mealsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get().await()

            val meals = snapshot.toObjects(Meal::class.java)
            Log.d("NutritionRepo", "Tìm thấy ${meals.size} món ăn")

            Result.success(meals)
        } catch (e: Exception) {
            Log.e("NutritionRepo", "LỖI LẤY DỮ LIỆU: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- Helper Functions giữ nguyên ---
    private fun getDateId(date: Date): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(date)
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.time
    }

    private fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.time
    }
}