package com.example.fitlifesmarthealthlifestyleapp.domain.model

import com.google.firebase.Timestamp

data class ActivityLog(
    var id: String = "", // Format: userId_timestamp
    var userId: String = "",
    var activityType: String = "Running", // Running, Walking, Cycling
    var startTime: Timestamp = Timestamp.now(),
    var endTime: Timestamp = Timestamp.now(),
    var durationSeconds: Int = 0,
    var distanceKm: Double = 0.0,
    var avgSpeedKmh: Double = 0.0,
    var paceMinPerKm: String = "0:00",
    var caloriesBurned: Int = 0,
    var createdAt: Timestamp = Timestamp.now()
)
