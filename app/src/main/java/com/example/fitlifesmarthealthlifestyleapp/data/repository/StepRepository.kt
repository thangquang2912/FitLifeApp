package com.example.fitlifesmarthealthlifestyleapp.data.repository

import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun updateSteps(uid: String, steps: Int) {
        try {
            val dateId = DateUtils.getCurrentDateId()
            val data = mapOf(
                "userId" to uid,
                "dateId" to dateId,
                "steps" to steps,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            db.collection("daily_steps")
                .document("${uid}_$dateId")
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWeeklySteps(uid: String): Result<List<Map<String, Any>>> {
        return try {
            val calendar = Calendar.getInstance()
            // SỬA LỖI: Đổi định dạng từ yyyyMMdd thành yyyy-MM-dd để khớp với Firestore
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

            Log.d("WEEKLY_STEPS", "docs size = ${snapshots.size()}")


            val results = snapshots.documents.mapNotNull { it.data }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
