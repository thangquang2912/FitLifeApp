package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

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

    suspend fun getWeeklyWaterLogs(userId: String): Result<List<WaterLog>> {
        return try {
            // Lấy 7 ngày gần nhất
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateIds = mutableListOf<String>()
            
            for (i in 0 until 7) {
                dateIds.add(sdf.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val snapshots = usersCollection.document(userId)
                .collection("water_logs")
                .whereIn("__name__", dateIds)
                .get().await()

            val logs = snapshots.toObjects(WaterLog::class.java)
            // Sắp xếp lại theo thứ tự ngày tăng dần
            val sortedLogs = logs.sortedBy { it.id }
            
            Result.success(sortedLogs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
