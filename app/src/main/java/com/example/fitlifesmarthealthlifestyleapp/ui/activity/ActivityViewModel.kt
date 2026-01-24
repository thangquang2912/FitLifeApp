package com.example.fitlifesmarthealthlifestyleapp.ui.activity

import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.ActivityRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LatLngPoint
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class ActivityViewModel : ViewModel() {
    private val activityRepository = ActivityRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ActivityViewModel"

    private val _isTracking = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _distance = MutableLiveData(0.0) // km
    val distance: LiveData<Double> = _distance

    private val _speed = MutableLiveData(0.0) // km/h (instant/derived)
    val speed: LiveData<Double> = _speed

    private val _duration = MutableLiveData(0) // seconds
    val duration: LiveData<Int> = _duration

    private val _calories = MutableLiveData(0)
    val calories: LiveData<Int> = _calories

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _routePoints = MutableLiveData<List<LatLngPoint>>(emptyList())
    val routePoints: LiveData<List<LatLngPoint>> = _routePoints

    private var startTime: Long = 0
    private var userWeightKg: Float = 70f

    // smooth speed to reduce spikes
    private var smoothedSpeedKmh: Double = 0.0

    init {
        loadUserWeight()
    }

    private fun loadUserWeight() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = userRepository.getUserDetails(uid)
            if (result.isSuccess) {
                userWeightKg = result.getOrNull()?.weight ?: 70f
                Log.d(TAG, "User weight loaded: $userWeightKg kg")
            }
        }
    }

    fun startTracking() {
        _isTracking.value = true
        startTime = System.currentTimeMillis()
        _distance.value = 0.0
        _speed.value = 0.0
        smoothedSpeedKmh = 0.0
        _duration.value = 0
        _calories.value = 0
        _routePoints.value = emptyList()
    }

    fun stopTracking() {
        _isTracking.value = false
        Log.d(TAG, "Tracking stopped")
    }

    fun updateFromService(
        location: Location,
        distanceKm: Double,
        speedKmh: Double,
        route: List<LatLngPoint>
    ) {
        _currentLocation.value = location
        _distance.value = distanceKm

        // smooth speed: EMA
        smoothedSpeedKmh = if (smoothedSpeedKmh == 0.0) speedKmh else (0.25 * speedKmh + 0.75 * smoothedSpeedKmh)
        _speed.value = smoothedSpeedKmh

        _routePoints.value = route
    }

    fun updateDuration(seconds: Int) {
        _duration.value = seconds
        calculateCalories()
    }

    /**
     * Calories for running:
     * ACSM running equation (flat):
     * VO2 = 3.5 + 0.2*speed(m/min)
     * kcal/min ≈ VO2 * weight(kg)/1000 * 5
     *
     * We use speed derived from distance & duration for stability:
     * avgSpeedKmh = distanceKm / hours
     */
    private fun calculateCalories() {
        val distanceKm = _distance.value ?: 0.0
        val durationSec = _duration.value ?: 0
        if (durationSec <= 0) {
            _calories.value = 0
            return
        }

        val hours = durationSec / 3600.0
        val avgSpeedKmh = if (hours > 0) (distanceKm / hours) else 0.0

        // clamp running realistic range to avoid spikes (walking->running)
        val clampedKmh = avgSpeedKmh.coerceIn(3.0, 20.0)

        val speedMPerMin = (clampedKmh * 1000.0) / 60.0
        val vo2 = 3.5 + 0.2 * speedMPerMin

        val kcalPerMin = vo2 * userWeightKg / 1000.0 * 5.0
        val minutes = durationSec / 60.0
        val kcal = (kcalPerMin * minutes)

        // additional sanity: calories should not exceed (distanceKm*~120) for running
        val maxByDistance = distanceKm * 120.0
        val finalKcal = minOf(kcal, maxByDistance).roundToInt()

        _calories.value = max(0, finalKcal)
    }

    fun completeActivity() {
        val uid = auth.currentUser?.uid ?: run {
            _toastMessage.value = "Please login first"
            return
        }

        val distanceKm = _distance.value ?: 0.0
        val durationSec = _duration.value ?: 0
        val avgSpeedKmh = run {
            val hours = durationSec / 3600.0
            if (hours > 0) distanceKm / hours else 0.0
        }
        val calories = _calories.value ?: 0
        val route = _routePoints.value ?: emptyList()

        if (durationSec < 10 || route.size < 2) {
            _toastMessage.value = "Activity too short to save"
            return
        }

        val paceMinPerKm = if (distanceKm > 0) {
            val paceInMinutes = (durationSec / 60.0) / distanceKm
            val paceMin = paceInMinutes.toInt()
            val paceSec = ((paceInMinutes - paceMin) * 60).toInt()
            String.format("%d:%02d", paceMin, paceSec)
        } else "0:00"

        val log = ActivityLog(
            id = "${uid}_${System.currentTimeMillis()}",
            userId = uid,
            activityType = "Running",
            startTime = Timestamp(java.util.Date(startTime)),
            endTime = Timestamp.now(),
            durationSeconds = durationSec,
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeedKmh,
            paceMinPerKm = paceMinPerKm,
            caloriesBurned = calories,
            routePoints = route
        )

        viewModelScope.launch {
            val result = activityRepository.saveActivityLog(log)
            if (result.isSuccess) {
                _toastMessage.value = "Activity saved!"
                resetStats()
            } else {
                _toastMessage.value = "Failed to save: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private fun resetStats() {
        _distance.value = 0.0
        _speed.value = 0.0
        smoothedSpeedKmh = 0.0
        _duration.value = 0
        _calories.value = 0
        _routePoints.value = emptyList()
    }
}