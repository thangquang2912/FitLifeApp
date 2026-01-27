package com.example.fitlifesmarthealthlifestyleapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fitlifesmarthealthlifestyleapp.MainActivity
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.service.StepSensorManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class StepCounterService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var stepSensorManager: StepSensorManager? = null
    private val stepRepository = StepRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Khởi tạo và lắng nghe cảm biến ngay cả khi app bị tắt
        stepSensorManager = StepSensorManager(this) { deltaSteps ->
            updateStepsInFirestore(deltaSteps)
        }
        stepSensorManager?.startListening()
    }

    private fun updateStepsInFirestore(delta: Int) {
        val uid = auth.currentUser?.uid ?: return
        serviceScope.launch {
            val currentSteps = stepRepository.getTodaySteps(uid)
            stepRepository.updateSteps(uid, currentSteps + delta)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Đảm bảo hệ thống sẽ khởi động lại service nếu bị kill
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stepSensorManager?.stopListening()
        serviceScope.cancel()
    }

    private fun createNotification(): Notification {
        val channelId = "step_counter_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Step Counter", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FitLife Step Counter")
            .setContentText("Đang đếm số bước chân của bạn...")
            .setSmallIcon(R.drawable.ic_steps)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
