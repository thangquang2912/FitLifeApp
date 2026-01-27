package com.example.fitlifesmarthealthlifestyleapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.NutritionRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

class CaloriesReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val auth = FirebaseAuth.getInstance()
    private val nutritionRepository = NutritionRepository()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()

        val today = Date()
        val summary = nutritionRepository.getDailySummary(uid, today)
        val goal = nutritionRepository.getUserCalorieGoal(uid)

        val consumedCalories =
            summary?.get("totalCalories")?.toString()?.toIntOrNull() ?: 0

        if (consumedCalories < goal * 0.8) {
            val remaining = goal - consumedCalories

            sendNotification(
                "Calories reminder 🍽️",
                "You still need about $remaining kcal today."
            )
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "calories_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Calories Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_calories) // nhớ có icon này
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}
