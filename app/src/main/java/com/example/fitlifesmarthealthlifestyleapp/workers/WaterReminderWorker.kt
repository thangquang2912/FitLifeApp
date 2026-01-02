package com.example.fitlifesmarthealthlifestyleapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth

class WaterReminderWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams){
    private val waterRepository = WaterRepository() // Khởi tạo Repository
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.failure()
        val todayId = DateUtils.getCurrentDateId()

        // 1. Lấy dữ liệu log nước hôm nay từ Firestore
        val result = waterRepository.getWaterLog(uid, todayId)

        if (result.isSuccess) {
            val log = result.getOrNull()

            // 2. Logic kiểm tra: Nếu chưa có log HOẶC uống dưới 80% mục tiêu
            if (log == null || log.currentIntake < log.dailyGoal * 0.8) {

                val missingAmount = if (log == null) 2000 else (log.dailyGoal - log.currentIntake)

                // 3. Gửi thông báo
                sendNotification("Don't forget to drink water! \uD83D\uDCA7", "You need to drink ${missingAmount}ml to keep your goal.")
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "water_reminder_channel"

        // Tạo Channel (Bắt buộc cho Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Water Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo thông báo
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_water) // Đảm bảo bạn có icon này
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}