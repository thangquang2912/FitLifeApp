package com.example.fitlifesmarthealthlifestyleapp.data.repository

import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "UserRepository"

    // Hàm lấy thông tin User
    suspend fun getUserDetails(uid: String): Result<User> {
        return try {
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Data parsing error"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Hàm cập nhật thông tin User
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Result.success(Unit) // Báo thành công
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e) // Báo lỗi kèm Exception
        }
    }

    suspend fun updateUserWaterGoal(uid: String, newGoal: Int): Result<Boolean> {
        return try {
            db.collection("users").document(uid)
                .update("dailyWaterGoal", newGoal)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserGoals(
        uid: String,
        dailyWaterGoal: Int,
        dailyStepsGoal:  Int,
        dailyActiveCalories: Int,
        dailyCaloriesConsume: Int,
        weeklyRunning: Int
    ): Result<Unit> {
        return try {
            val goalsMap = mapOf(
                "dailyWaterGoal" to dailyWaterGoal,
                "dailyStepsGoal" to dailyStepsGoal,
                "dailyActiveCalories" to dailyActiveCalories,
                "dailyCaloriesConsume" to dailyCaloriesConsume,
                "weeklyRunning" to weeklyRunning
            )

            // Dùng set() với merge() thay vì update()
            val task = db.collection("users")
                .document(uid)
                .set(goalsMap, SetOptions.merge())
            task.await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    // Hàm lắng nghe dữ liệu User thay đổi theo thời gian thực
    fun getUserDetailsStream(uid: String): Flow<User?> = callbackFlow {
        // 1. Đăng ký listener với Firestore
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Đóng dòng chảy nếu lỗi
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(user) // Bắn dữ liệu mới vào dòng chảy
                } else {
                    trySend(null)
                }
            }

        // 2. Hủy đăng ký khi ViewModel không cần nữa (để tránh rò rỉ bộ nhớ)
        awaitClose {
            Log.d(TAG, "getUserDetailsStream: Removing listener")
            registration.remove() }
    }

    // Hàm hỗ trợ lấy ID theo ngày (Format: YYYY-MM-DD)
    private fun getCurrentDateId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // 1. Lưu Calo: Cộng dồn vào document của ngày hôm nay
    suspend fun logCalorieBurn(uid: String, calories: Int): Result<Unit> {
        return try {
            val dateId = getCurrentDateId()

            // Dùng FieldValue.increment để cộng dồn nguyên tử (Atomic increment)
            // Không cần quan tâm file đã tồn tại hay chưa
            val data = hashMapOf(
                "totalCalories" to FieldValue.increment(calories.toLong()),
                "lastUpdated" to System.currentTimeMillis()
            )

            db.collection("users").document(uid)
                .collection("calories_logs")
                .document(dateId) // Key chính là ngày hôm nay
                .set(data, SetOptions.merge()) // Merge: Chỉ update trường totalCalories, giữ nguyên các trường khác nếu có
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Lấy tổng Calo hôm nay: Chỉ cần get đúng document ID ngày hôm nay
    suspend fun getTodayActiveCalories(uid: String): Result<Int> {
        return try {
            val dateId = getCurrentDateId()

            val document = db.collection("users").document(uid)
                .collection("calories_logs")
                .document(dateId)
                .get()
                .await()

            if (document.exists()) {
                // Lấy giá trị totalCalories, nếu null thì trả về 0
                val total = document.getLong("totalCalories")?.toInt() ?: 0
                Result.success(total)
            } else {
                // Chưa có log hôm nay -> 0
                Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}