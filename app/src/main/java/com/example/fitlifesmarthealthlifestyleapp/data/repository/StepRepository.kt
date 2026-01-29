package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    // Lắng nghe số bước chân hôm nay Real-time
    fun getTodayStepsStream(uid: String): Flow<Int> = callbackFlow {
        val dateId = DateUtils.getCurrentDateId()
        val registration = db.collection("daily_steps")
            .document("${uid}_$dateId")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val steps = snapshot?.getLong("steps")?.toInt() ?: 0
                trySend(steps)
            }
        awaitClose { registration.remove() }
    }

    suspend fun incrementSteps(uid: String, delta: Int) {
        try {
            val dateId = DateUtils.getCurrentDateId()
            val data = hashMapOf(
                "userId" to uid,
                "dateId" to dateId,
                "steps" to FieldValue.increment(delta.toLong()),
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            db.collection("daily_steps")
                .document("${uid}_$dateId")
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWeeklyStepsStream(uid: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateIds = mutableListOf<String>()
        for (i in 0 until 7) {
            val date = sdf.format(calendar.time)
            dateIds.add("${uid}_$date")
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val registration = db.collection("daily_steps")
            .whereEqualTo("userId", uid)
            .whereIn("__name__", dateIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val data = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(data)
            }
        
        awaitClose { registration.remove() }
    }
}
