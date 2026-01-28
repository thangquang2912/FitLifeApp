package com.example.fitlifesmarthealthlifestyleapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
import com.google.firebase.auth.FirebaseAuth

class StepsReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val auth = FirebaseAuth.getInstance()
    private val stepRepository = StepRepository()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()

        val todaySteps = stepRepository.getTodaySteps(uid)
        val goal = 8000

        if (todaySteps < goal * 0.8) {
            val remaining = goal - todaySteps

            sendNotification(
                "Step reminder 🚶‍♂️",
                "Only $remaining steps left to reach your goal!"
            )
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "step_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Step Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_steps) // nhớ có icon này
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        manager.notify(3001, notification)
    }
}
