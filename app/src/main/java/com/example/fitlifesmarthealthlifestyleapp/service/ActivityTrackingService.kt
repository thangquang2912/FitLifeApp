package com.example.fitlifesmarthealthlifestyleapp.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.gms.location.*

class ActivityTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var isTracking = false
    private var lastLocation: Location? = null
    private var totalDistance = 0.0 // in meters

    private var listener: TrackingListener? = null

    interface TrackingListener {
        fun onLocationUpdate(location: Location, distanceKm: Double, speedKmh: Double)
    }

    inner class LocalBinder : Binder() {
        fun getService(): ActivityTrackingService = this@ActivityTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setListener(listener: TrackingListener) {
        this.listener = listener
    }

    fun startTracking() {
        if (isTracking) return

        Log.d(TAG, "Starting location tracking")
        isTracking = true
        totalDistance = 0.0
        lastLocation = null

        startForeground(NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
    }

    fun stopTracking(): Double {
        if (!isTracking) return 0.0

        Log.d(TAG, "Stopping location tracking")
        isTracking = false

        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return totalDistance / 1000.0 // Convert to km
    }

    fun getCurrentDistance(): Double {
        return totalDistance / 1000.0 // in km
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun handleLocationUpdate(newLocation: Location) {
        if (! isTracking) return

        lastLocation?.let { last ->
            val distance = last.distanceTo(newLocation)

            // Only count if moved more than 5 meters (filter GPS noise)
            if (distance > 5f) {
                totalDistance += distance

                val speedKmh = if (newLocation.hasSpeed()) {
                    (newLocation.speed * 3.6).toDouble() // m/s to km/h
                } else {
                    0.0
                }

                listener?.onLocationUpdate(newLocation, totalDistance / 1000.0, speedKmh)
                Log.d(TAG, "Distance: %.2f km, Speed: %.1f km/h".format(totalDistance / 1000.0, speedKmh))
            }
        }

        lastLocation = newLocation
    }

    private fun createNotification(): android.app.Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, com.example.fitlifesmarthealthlifestyleapp.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Activity Tracking")
            .setContentText("Tracking your activity...")
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "ActivityTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "activity_tracking_channel"
        private const val UPDATE_INTERVAL_MS = 2000L // 2 seconds
        private const val FASTEST_INTERVAL_MS = 1000L // 1 second
    }
}