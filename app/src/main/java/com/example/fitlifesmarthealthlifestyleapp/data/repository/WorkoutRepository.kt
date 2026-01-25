package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.WorkoutProgram
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WorkoutRepository {
    private val db = FirebaseFirestore.getInstance()
    private val programCollection = db.collection("workout_programs")

    // Lấy tất cả chương trình tập
    suspend fun getWorkoutPrograms(): Result<List<WorkoutProgram>> {
        return try {
            val snapshot = programCollection.get().await()
            val programs = snapshot.toObjects(WorkoutProgram::class.java)
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // (Tuỳ chọn) Lọc theo mục tiêu
    suspend fun getProgramsByGoal(goal: String): Result<List<WorkoutProgram>> {
        return try {
            val snapshot = programCollection.whereEqualTo("goal", goal).get().await()
            val programs = snapshot.toObjects(WorkoutProgram::class.java)
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}