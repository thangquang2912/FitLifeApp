package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}
