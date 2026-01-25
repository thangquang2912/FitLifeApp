package com.example.fitlifesmarthealthlifestyleapp.data.repository

import com.example.fitlifesmarthealthlifestyleapp.domain.model.WorkoutProgram
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Exercise

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

    suspend fun getProgramsByGoal(goal: String): Result<List<WorkoutProgram>> {
        return try {
            val snapshot = programCollection.whereEqualTo("goal", goal).get().await()
            val programs = snapshot.toObjects(WorkoutProgram::class.java)
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExercisesByProgramId(programId: String): Result<List<Exercise>> {
        return try {
            val snapshot = programCollection.document(programId).collection("exercises")
                .orderBy("order")
                .get()
                .await()

            val exercises = snapshot.toObjects(Exercise::class.java)
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}