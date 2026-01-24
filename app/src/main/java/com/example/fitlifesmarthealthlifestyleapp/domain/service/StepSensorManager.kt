package com.example.fitlifesmarthealthlifestyleapp.domain.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast

class StepSensorManager(private val context: Context, private val onStepCountChanged: (Int) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private var initialStepsAtSensorStart = -1

    fun startListening() {
        if (stepSensor == null) {
            Log.e("StepSensorManager", "Thiết bị không hỗ trợ cảm biến bước chân!")
            Toast.makeText(context, "Thiết bị này không hỗ trợ đếm bước chân phần cứng", Toast.LENGTH_LONG).show()
            return
        }
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        initialStepsAtSensorStart = -1
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            Log.d("StepSensorManager", "Sensor changed: $totalStepsSinceBoot")
            
            if (initialStepsAtSensorStart == -1) {
                initialStepsAtSensorStart = totalStepsSinceBoot
                return
            }
            
            val stepsSinceStarted = totalStepsSinceBoot - initialStepsAtSensorStart
            
            if (stepsSinceStarted > 0) {
                Log.d("StepSensorManager", "Steps delta detected: $stepsSinceStarted")
                onStepCountChanged(stepsSinceStarted)
                initialStepsAtSensorStart = totalStepsSinceBoot
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
