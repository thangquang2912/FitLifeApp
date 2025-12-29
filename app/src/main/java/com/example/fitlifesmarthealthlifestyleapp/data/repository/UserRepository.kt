package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
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
}