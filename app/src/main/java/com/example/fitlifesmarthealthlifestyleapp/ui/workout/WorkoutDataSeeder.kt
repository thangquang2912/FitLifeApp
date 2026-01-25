package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Exercise
import com.google.firebase.firestore.FirebaseFirestore

object WorkoutDataSeeder {

    fun seedAllExercises() {
        val db = FirebaseFirestore.getInstance()

        // 1. TẠO BỘ DỮ LIỆU ĐỘNG TÁC CHO TẤT CẢ CÁC BÀI TẬP
        val allExercisesData = mapOf(
            // --- WEIGHT LOSS ---
            "HIIT Cardio Burn" to listOf(
                Exercise("", "High Knees", 30, 10, "https://media.giphy.com/media/l41lZk6E5j4wS0gxi/giphy.gif", 1),
                Exercise("", "Mountain Climbers", 30, 10, "https://media.giphy.com/media/xT0BekoZ6kLq6E4pJS/giphy.gif", 2),
                Exercise("", "Burpees", 30, 15, "https://media.giphy.com/media/23hPPMRgPxbNBlPQe3/giphy.gif", 3),
                Exercise("", "Plank Jacks", 30, 15, "https://media.giphy.com/media/3o7qDWp7hxba1rcfYA/giphy.gif", 4)
            ),
            "Full Body Fat Blast" to listOf(
                Exercise("", "Jumping Jacks", 30, 15, "https://media.giphy.com/media/cO8m0b5K3VpPif0IqU/giphy.gif", 1),
                Exercise("", "Bodyweight Squats", 45, 15, "https://media.giphy.com/media/1qfKUnOWRw0qSO4mWG/giphy.gif", 2),
                Exercise("", "Push-ups", 30, 20, "https://media.giphy.com/media/VbnGZ9117xYkXo68bH/giphy.gif", 3),
                Exercise("", "Lunges", 40, 20, "https://media.giphy.com/media/1xVbZRmHIt1r1n1vJg/giphy.gif", 4)
            ),
            "Circuit Shred" to listOf(
                Exercise("", "Skaters", 40, 10, "https://media.giphy.com/media/xT8qBfGqjR8A0T1xYQ/giphy.gif", 1),
                Exercise("", "Bicycle Crunches", 45, 10, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 2),
                Exercise("", "Tricep Dips", 30, 10, "https://media.giphy.com/media/wMxhZ4P8XQO5e/giphy.gif", 3)
            ),
            "Morning Metabolism" to listOf(
                Exercise("", "Cat-Cow Stretch", 60, 0, "https://media.giphy.com/media/3o7btT1T9qpQZWhNlK/giphy.gif", 1),
                Exercise("", "Bird Dog", 45, 10, "https://media.giphy.com/media/1k00pQ8GZcw6ZfE0gS/giphy.gif", 2),
                Exercise("", "Glute Bridges", 45, 10, "https://media.giphy.com/media/7ZWee4mC3K9YhG6k5R/giphy.gif", 3)
            ),

            // --- MUSCLE GAIN ---
            "Push-Up Master" to listOf(
                Exercise("", "Standard Push-ups", 40, 20, "https://media.giphy.com/media/VbnGZ9117xYkXo68bH/giphy.gif", 1),
                Exercise("", "Wide Grip Push-ups", 30, 20, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 2),
                Exercise("", "Diamond Push-ups", 20, 30, "https://media.giphy.com/media/xT8qBwJ8z7f5Jt1H8s/giphy.gif", 3)
            ),
            "Leg Day Legend" to listOf(
                Exercise("", "Sumo Squats", 45, 20, "https://media.giphy.com/media/1qfKUnOWRw0qSO4mWG/giphy.gif", 1),
                Exercise("", "Walking Lunges", 40, 20, "https://media.giphy.com/media/1xVbZRmHIt1r1n1vJg/giphy.gif", 2),
                Exercise("", "Calf Raises", 50, 20, "https://media.giphy.com/media/3o7qDWp7hxba1rcfYA/giphy.gif", 3)
            ),
            "Upper Body Pump" to listOf(
                Exercise("", "Pike Push-ups", 30, 30, "https://media.giphy.com/media/VbnGZ9117xYkXo68bH/giphy.gif", 1),
                Exercise("", "Arm Circles", 60, 10, "https://media.giphy.com/media/xT8qBfGqjR8A0T1xYQ/giphy.gif", 2),
                Exercise("", "Plank to Push-up", 30, 20, "https://media.giphy.com/media/VbnGZ9117xYkXo68bH/giphy.gif", 3)
            ),
            "Pull & Core Strength" to listOf(
                Exercise("", "Superman", 45, 15, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 1),
                Exercise("", "Leg Raises", 40, 20, "https://media.giphy.com/media/xT8qBwJ8z7f5Jt1H8s/giphy.gif", 2),
                Exercise("", "Side Plank", 30, 15, "https://media.giphy.com/media/xT8qBfGqjR8A0T1xYQ/giphy.gif", 3)
            ),
            "Lower Body Power" to listOf(
                Exercise("", "Bulgarian Split Squats", 30, 20, "https://media.giphy.com/media/1xVbZRmHIt1r1n1vJg/giphy.gif", 1),
                Exercise("", "Jump Squats", 30, 20, "https://media.giphy.com/media/1qfKUnOWRw0qSO4mWG/giphy.gif", 2),
                Exercise("", "Wall Sit", 45, 15, "https://media.giphy.com/media/3o7qDWp7hxba1rcfYA/giphy.gif", 3)
            ),

            // --- YOGA ---
            "Morning Yoga Flow" to listOf(
                Exercise("", "Child's Pose", 60, 0, "https://media.giphy.com/media/7ZWee4mC3K9YhG6k5R/giphy.gif", 1),
                Exercise("", "Downward Dog", 60, 0, "https://media.giphy.com/media/1k00pQ8GZcw6ZfE0gS/giphy.gif", 2),
                Exercise("", "Cobra Pose", 45, 10, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 3)
            ),
            "Sun Salutation" to listOf(
                Exercise("", "Mountain Pose", 30, 0, "https://media.giphy.com/media/3o7btT1T9qpQZWhNlK/giphy.gif", 1),
                Exercise("", "Upward Salute", 30, 0, "https://media.giphy.com/media/1k00pQ8GZcw6ZfE0gS/giphy.gif", 2),
                Exercise("", "Chaturanga", 20, 10, "https://media.giphy.com/media/VbnGZ9117xYkXo68bH/giphy.gif", 3)
            ),
            "Vinyasa Power Flow" to listOf(
                Exercise("", "Warrior I", 45, 0, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 1),
                Exercise("", "Warrior II", 45, 0, "https://media.giphy.com/media/1xVbZRmHIt1r1n1vJg/giphy.gif", 2),
                Exercise("", "Triangle Pose", 45, 10, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 3)
            ),
            "Balance & Core" to listOf(
                Exercise("", "Tree Pose", 40, 10, "https://media.giphy.com/media/3o7btT1T9qpQZWhNlK/giphy.gif", 1),
                Exercise("", "Boat Pose", 30, 15, "https://media.giphy.com/media/7ZWee4mC3K9YhG6k5R/giphy.gif", 2),
                Exercise("", "Plank", 60, 20, "https://media.giphy.com/media/xT8qBfGqjR8A0T1xYQ/giphy.gif", 3)
            ),
            "Deep Stretch" to listOf(
                Exercise("", "Pigeon Pose", 60, 0, "https://media.giphy.com/media/1k00pQ8GZcw6ZfE0gS/giphy.gif", 1),
                Exercise("", "Butterfly Stretch", 60, 0, "https://media.giphy.com/media/7ZWee4mC3K9YhG6k5R/giphy.gif", 2),
                Exercise("", "Happy Baby", 60, 0, "https://media.giphy.com/media/3o7btXkbsV26U95Uly/giphy.gif", 3)
            )
        )

        Log.d("Seeder", "Bắt đầu tải danh sách bài tập từ Firestore...")

        db.collection("workout_programs").get()
            .addOnSuccessListener { querySnapshot ->
                for (programDoc in querySnapshot.documents) {
                    val programId = programDoc.id
                    val programName = programDoc.getString("name") ?: ""

                    val exercisesForThisProgram = allExercisesData[programName]

                    if (exercisesForThisProgram != null) {
                        val exercisesCollection = db.collection("workout_programs")
                            .document(programId)
                            .collection("exercises")

                        for (exercise in exercisesForThisProgram) {
                            exercisesCollection.add(exercise)
                                .addOnSuccessListener { docRef ->
                                    exercisesCollection.document(docRef.id).update("id", docRef.id)
                                }
                        }
                        Log.d("Seeder", "Đã thêm động tác cho: $programName")
                    }
                }
                Log.d("Seeder", "✅ HOÀN TẤT SEED TOÀN BỘ DỮ LIỆU!")
            }
            .addOnFailureListener { e ->
                Log.e("Seeder", "Lỗi: ${e.message}")
            }
    }
}