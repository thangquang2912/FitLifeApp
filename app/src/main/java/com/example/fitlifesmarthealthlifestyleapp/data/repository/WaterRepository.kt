package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class WaterRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Lấy log nước của một ngày cụ thể
    suspend fun getWaterLog(userId: String, dateId: String): Result<WaterLog?> {
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("water_logs")
                .document(dateId)
                .get().await()

            if (snapshot.exists()) {
                val log = snapshot.toObject(WaterLog::class.java)
                log?.id = snapshot.id // Đảm bảo ID được gán
                Result.success(log)
            } else {
                Result.success(null) // Chưa có log cho ngày này
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cập nhật log nước
    suspend fun saveWaterLog(log: WaterLog): Result<Boolean> {
        return try {
            usersCollection.document(log.userId)
                .collection("water_logs")
                .document(log.id) // ID là ngày (VD: 2025-12-30)
                .set(log, SetOptions.merge()) // Merge để không mất data cũ nếu có
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}