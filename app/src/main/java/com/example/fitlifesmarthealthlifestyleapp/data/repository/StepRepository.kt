package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StepRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getTodaySteps(uid: String): Int {
        return try {
            val dateId = DateUtils.getCurrentDateId()
            val document = db.collection("daily_steps")
                .document("${uid}_$dateId")
                .get()
                .await()
            
            if (document.exists()) {
                document.getLong("steps")?.toInt() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    // Hàm cộng dồn số bước chân (Dùng FieldValue.increment để tránh ghi đè dữ liệu cũ)
    suspend fun incrementSteps(uid: String, delta: Int) {
        try {
            val dateId = DateUtils.getCurrentDateId()
            val data = hashMapOf(
                "userId" to uid,
                "dateId" to dateId,
                "steps" to FieldValue.increment(delta.toLong()),
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            // Dùng set với merge để tạo mới nếu chưa có hoặc cộng dồn nếu đã có
            db.collection("daily_steps")
                .document("${uid}_$dateId")
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWeeklySteps(uid: String): Result<List<Map<String, Any>>> {
        return try {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateIds = mutableListOf<String>()
            
            for (i in 0 until 7) {
                val date = sdf.format(calendar.time)
                dateIds.add("${uid}_$date")
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val snapshots = db.collection("daily_steps")
                .whereEqualTo("userId", uid)
                .whereIn("__name__", dateIds)
                .get().await()

            val results = snapshots.documents.mapNotNull { it.data }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
