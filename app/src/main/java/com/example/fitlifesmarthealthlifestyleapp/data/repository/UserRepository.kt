package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

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
        awaitClose { registration.remove() }
    }
}