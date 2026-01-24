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
import com.example.fitlifesmarthealthlifestyleapp.MainActivity
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LatLngPoint
import com.google.android.gms.location.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class ActivityTrackingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var isTracking = false
    private var lastAcceptedLocation: Location? = null
    private var totalDistanceMeters = 0.0

    private val routePoints = CopyOnWriteArrayList<LatLngPoint>()

    private var listener: TrackingListener? = null

    interface TrackingListener {
        fun onLocationUpdate(
            location: Location,
            distanceKm: Double,
            speedKmh: Double,
            routePoints: List<LatLngPoint>
        )
    }

    inner class LocalBinder : Binder() {
        fun getService(): ActivityTrackingService = this@ActivityTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { handleLocationUpdate(it) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setListener(listener: TrackingListener) {
        this.listener = listener
    }

    fun startTracking() {
        if (isTracking) return

        isTracking = true
        totalDistanceMeters = 0.0
        lastAcceptedLocation = null
        routePoints.clear()

        startForeground(NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
    }

    fun stopTracking(): Double {
        if (!isTracking) return 0.0
        isTracking = false

        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return totalDistanceMeters / 1000.0
    }

    fun getRoutePoints(): List<LatLngPoint> = routePoints.toList()

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        // Để ổn định hơn: BALANCED hoặc HIGH_ACCURACY tùy bạn
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun handleLocationUpdate(newLocation: Location) {
        if (!isTracking) return

        // ---- 1) Basic accuracy filter ----
        if (newLocation.hasAccuracy() && newLocation.accuracy > MAX_ACCEPTED_ACCURACY_M) {
            Log.w(TAG, "Drop point due to low accuracy: ${newLocation.accuracy}m")
            return
        }

        val last = lastAcceptedLocation

        // ---- 2) Jump filter (teleport) ----
        if (last != null) {
            val dtSec = max(1.0, (newLocation.time - last.time) / 1000.0)
            val dMeters = last.distanceTo(newLocation).toDouble()

            // speed derived from distance/time, more stable than location.speed sometimes
            val derivedSpeedMps = dMeters / dtSec

            // nếu nhảy quá xa trong thời gian ngắn -> bỏ
            if (dMeters > MAX_JUMP_DISTANCE_M && derivedSpeedMps > MAX_JUMP_SPEED_MPS) {
                Log.w(TAG, "Drop point due to GPS jump: d=${"%.1f".format(dMeters)}m dt=${"%.1f".format(dtSec)}s v=${"%.1f".format(derivedSpeedMps)}m/s")
                return
            }

            // ---- 3) Accept distance if moved enough ----
            if (dMeters >= MIN_MOVE_TO_COUNT_M) {
                totalDistanceMeters += dMeters
            } else {
                // nếu đứng yên, vẫn cho update UI speed=0 nhưng không add point để khỏi răng cưa
                notifyListener(newLocation, 0.0)
                return
            }
        }

        // ---- 4) Accept point ----
        val p = LatLngPoint(
            lat = newLocation.latitude,
            lng = newLocation.longitude,
            timeMs = System.currentTimeMillis(),
            accuracyMeters = if (newLocation.hasAccuracy()) newLocation.accuracy else 999f
        )
        routePoints.add(p)

        // speedKmh dùng derived nếu có last; fallback location.speed
        val speedKmh = if (last != null) {
            val dtSec = max(1.0, (newLocation.time - last.time) / 1000.0)
            val dMeters = last.distanceTo(newLocation).toDouble()
            ((dMeters / dtSec) * 3.6)
        } else {
            if (newLocation.hasSpeed()) (newLocation.speed * 3.6).toDouble() else 0.0
        }

        lastAcceptedLocation = newLocation
        notifyListener(newLocation, speedKmh)
    }

    private fun notifyListener(location: Location, speedKmh: Double) {
        listener?.onLocationUpdate(
            location = location,
            distanceKm = totalDistanceMeters / 1000.0,
            speedKmh = speedKmh,
            routePoints = routePoints.toList()
        )
    }

    private fun createNotification(): android.app.Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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

        private const val UPDATE_INTERVAL_MS = 2000L
        private const val FASTEST_INTERVAL_MS = 1000L

        // Filters
        private const val MAX_ACCEPTED_ACCURACY_M = 25f     // loại điểm quá sai
        private const val MIN_MOVE_TO_COUNT_M = 5.0        // lọc nhiễu đứng yên
        private const val MAX_JUMP_DISTANCE_M = 120.0      // nhảy quá xa
        private const val MAX_JUMP_SPEED_MPS = 12.0        // ~43 km/h (chạy bộ khó tới) => coi là jump
    }
}