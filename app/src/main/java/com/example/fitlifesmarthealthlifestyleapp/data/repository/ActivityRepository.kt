package com.example.fitlifesmarthealthlifestyleapp.data.repository

import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ActivityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ActivityRepository"

    suspend fun saveActivityLog(log: ActivityLog): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving activity log: ${log.id}")

            withTimeout(10000L) {
                db.collection("users")
                    .document(log.userId)
                    .collection("activity_logs")
                    .document(log.id)
                    .set(log, SetOptions.merge())
                    .await()
            }

            Log.d(TAG, "Activity log saved successfully ✅")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save activity log", e)
            Result.failure(e)
        }
    }

    suspend fun getActivityLogs(userId: String, limit: Int = 10): Result<List<ActivityLog>> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("activity_logs")
                    .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                val logs = snapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java) }
                Result.success(logs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load activity logs", e)
                Result.failure(e)
            }
        }

    // NEW: load detail by id
    suspend fun getActivityLogById(userId: String, logId: String): Result<ActivityLog> =
        withContext(Dispatchers.IO) {
            try {
                val doc = db.collection("users")
                    .document(userId)
                    .collection("activity_logs")
                    .document(logId)
                    .get()
                    .await()

                val log = doc.toObject(ActivityLog::class.java)
                if (log != null) Result.success(log) else Result.failure(IllegalStateException("Activity not found"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load activity log by id", e)
                Result.failure(e)
            }
        }
}