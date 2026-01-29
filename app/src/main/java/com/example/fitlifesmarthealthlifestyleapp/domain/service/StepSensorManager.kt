package com.example.fitlifesmarthealthlifestyleapp.domain.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepSensorManager(private val context: Context, private val onStepCountChanged: (Int) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("step_sensor_prefs", Context.MODE_PRIVATE)

    fun startListening() {
        if (stepSensor == null) return
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()

            // 1. Lấy giá trị cảm biến lần cuối app còn sống (lưu trong máy)
            val lastSensorValue = prefs.getInt("last_sensor_value", -1)

            if (lastSensorValue == -1) {
                saveSensorValue(totalStepsSinceBoot)
                return
            }

            // 2. Tính số bước chân đã đi "bù" trong lúc app tắt (Delta)
            val delta = if (totalStepsSinceBoot >= lastSensorValue) {
                totalStepsSinceBoot - lastSensorValue
            } else {
                // Trường hợp điện thoại vừa khởi động lại (Reboot)
                totalStepsSinceBoot
            }

            if (delta > 0) {
                // 3. Gửi số bước "bù" này lên HomeViewModel để cộng dồn vào Firestore
                onStepCountChanged(delta)

                // 4. Lưu lại mốc mới nhất
                saveSensorValue(totalStepsSinceBoot)
            }
        }
    }

    private fun saveSensorValue(value: Int) {
        prefs.edit().putInt("last_sensor_value", value).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
