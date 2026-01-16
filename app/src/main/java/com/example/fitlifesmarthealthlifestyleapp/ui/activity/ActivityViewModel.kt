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

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> = _distance

    private val _speed = MutableLiveData(0.0)
    val speed: LiveData<Double> = _speed

    private val _duration = MutableLiveData(0)
    val duration: LiveData<Int> = _duration

    private val _calories = MutableLiveData(0)
    val calories: LiveData<Int> = _calories

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    // NEW
    private val _routePoints = MutableLiveData<List<LatLngPoint>>(emptyList())
    val routePoints: LiveData<List<LatLngPoint>> = _routePoints

    private var startTime: Long = 0
    private var userWeight: Float = 70f

    init { loadUserWeight() }

    private fun loadUserWeight() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = userRepository.getUserDetails(uid)
            if (result.isSuccess) {
                userWeight = result.getOrNull()?.weight ?: 70f
                Log.d(TAG, "User weight loaded: $userWeight kg")
            }
        }
    }

    fun startTracking() {
        _isTracking.value = true
        startTime = System.currentTimeMillis()
        _distance.value = 0.0
        _speed.value = 0.0
        _duration.value = 0
        _calories.value = 0
        _routePoints.value = emptyList()
    }

    fun stopTracking() {
        _isTracking.value = false
    }

    fun updateFromService(
        location: Location,
        distanceKm: Double,
        speedKmh: Double,
        route: List<LatLngPoint>
    ) {
        _currentLocation.value = location
        _distance.value = distanceKm
        _speed.value = speedKmh
        _routePoints.value = route
    }

    fun updateDuration(seconds: Int) {
        _duration.value = seconds
        calculateCalories()
    }

    private fun calculateCalories() {
        val durationHours = (_duration.value ?: 0) / 3600.0
        val avgSpeed = _speed.value ?: 0.0

        val met = when {
            avgSpeed < 6.0 -> 6.0
            avgSpeed < 8.0 -> 8.3
            avgSpeed < 10.0 -> 9.8
            avgSpeed < 12.0 -> 11.0
            else -> 12.5
        }

        _calories.value = (met * userWeight * durationHours).roundToInt()
    }

    fun completeActivity() {
        val uid = auth.currentUser?.uid ?: run {
            _toastMessage.value = "Please login first"
            return
        }

        val distanceKm = _distance.value ?: 0.0
        val durationSec = _duration.value ?: 0
        val avgSpeed = _speed.value ?: 0.0
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
            avgSpeedKmh = avgSpeed,
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
        _duration.value = 0
        _calories.value = 0
        _routePoints.value = emptyList()
    }
}