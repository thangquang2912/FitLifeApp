package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.ActivityRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LatLngPoint
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.max
import android.location.Location

sealed class ActivityDetailsState {
    data object Loading : ActivityDetailsState()
    data class Success(val ui: ActivityDetailsUi) : ActivityDetailsState()
    data class Error(val message: String) : ActivityDetailsState()
}

data class ActivityDetailsUi(
    val title: String,
    val subtitle: String,
    val distanceText: String,
    val durationText: String,
    val avgSpeedText: String,
    val paceText: String,
    val caloriesText: String,
    val raw: ActivityLog,
    val speedData: SpeedChartData = SpeedChartData()
)

data class SpeedChartData(
    val maxSpeed: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val minSpeed: Double = 0.0,
    val speedSegments: List<SpeedSegment> = emptyList()
)

data class SpeedSegment(
    val timeLabel: String,  // e.g., "0-5", "5-10"
    val speedKmh: Double,
    val durationSec: Int
)

class ActivityDetailsViewModel : ViewModel() {

    private val repo = ActivityRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableLiveData<ActivityDetailsState>()
    val state: LiveData<ActivityDetailsState> = _state

    fun load(activityId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = ActivityDetailsState.Error("Please login first")
            return
        }

        _state.value = ActivityDetailsState.Loading

        viewModelScope.launch {
            val result = repo.getActivityLogById(uid, activityId)
            if (result.isSuccess) {
                val log = result.getOrNull()!!
                val speedData = calculateSpeedChartData(log.routePoints, log.durationSeconds)
                _state.value = ActivityDetailsState.Success(toUi(log, speedData))
            } else {
                _state.value = ActivityDetailsState.Error(result.exceptionOrNull()?.message ?: "Failed to load activity")
            }
        }
    }

    private fun calculateSpeedChartData(
        routePoints: List<LatLngPoint>,
        totalDuration: Int
    ): SpeedChartData {
        if (routePoints.size < 2) return SpeedChartData()

        val sortedPoints = routePoints.sortedBy { it.timeMs }
        val cleanPoints = filterRoutePoints(sortedPoints)

        if (cleanPoints.size < 2) return SpeedChartData()

        // Chia thành 5-8 khoảng tùy thời gian
        val intervalCount = when {
            totalDuration <= 300 -> 5  // ≤5 phút: 5 khoảng
            totalDuration <= 900 -> 6  // ≤15 phút: 6 khoảng
            totalDuration <= 1800 -> 7 // ≤30 phút: 7 khoảng
            else -> 8                  // >30 phút: 8 khoảng
        }

        val segmentDuration = totalDuration / intervalCount
        val segments = mutableListOf<SpeedSegment>()

        for (i in 0 until intervalCount) {
            val startTime = sortedPoints.first().timeMs + (i * segmentDuration * 1000L)
            val endTime = startTime + (segmentDuration * 1000L)

            val segmentPoints = cleanPoints.filter { point ->
                point.timeMs in startTime..endTime
            }

            val avgSpeed = calculateAverageSpeedForSegment(segmentPoints)

            // Tạo nhãn thời gian đơn giản
            val timeLabel = when {
                segmentDuration >= 60 -> {
                    val startMin = i * segmentDuration / 60
                    val endMin = (i + 1) * segmentDuration / 60
                    "${startMin}-${endMin}"
                }
                else -> {
                    val startSec = i * segmentDuration
                    val endSec = (i + 1) * segmentDuration
                    "${startSec}s-${endSec}s"
                }
            }

            segments.add(SpeedSegment(
                timeLabel = timeLabel,
                speedKmh = avgSpeed,
                durationSec = segmentDuration
            ))
        }

        // Tính toán max, avg, min
        val speeds = segments.map { it.speedKmh }
        val maxSpeed = speeds.maxOrNull() ?: 0.0
        val minSpeed = speeds.minOrNull() ?: 0.0
        val avgSpeed = speeds.average()

        return SpeedChartData(
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            minSpeed = minSpeed,
            speedSegments = segments
        )
    }

    private fun filterRoutePoints(points: List<LatLngPoint>): List<LatLngPoint> {
        if (points.size <= 2) return points

        val out = ArrayList<LatLngPoint>()
        var last: LatLngPoint? = null

        for (p in points) {
            if (p.accuracyMeters > 25f) continue

            val l = last
            if (l == null) {
                out.add(p)
                last = p
                continue
            }

            val d = distanceMeters(l, p)
            val dtSec = max(1.0, (p.timeMs - l.timeMs) / 1000.0)
            val v = d / dtSec

            if (d > 120.0 && v > 12.0) continue
            if (d < 2.0) continue

            out.add(p)
            last = p
        }
        return out
    }

    private fun calculateAverageSpeedForSegment(points: List<LatLngPoint>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        var totalTime = 0.0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val distance = distanceMeters(prev, curr)
            val time = (curr.timeMs - prev.timeMs) / 1000.0

            if (time > 0) {
                totalDistance += distance
                totalTime += time
            }
        }

        if (totalTime == 0.0) return 0.0

        val avgSpeedMps = totalDistance / totalTime
        return avgSpeedMps * 3.6 // Convert to km/h
    }

    private fun distanceMeters(a: LatLngPoint, b: LatLngPoint): Double {
        val res = FloatArray(1)
        Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, res)
        return res[0].toDouble()
    }

    private fun toUi(log: ActivityLog, speedData: SpeedChartData): ActivityDetailsUi {
        val df = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.US)
        val subtitle = df.format(log.startTime.toDate())

        val avgSpeed = if (log.durationSeconds > 0) {
            val hours = log.durationSeconds / 3600.0
            if (hours > 0) log.distanceKm / hours else 0.0
        } else log.avgSpeedKmh

        return ActivityDetailsUi(
            title = "Activity Details",
            subtitle = subtitle,
            distanceText = "${String.format("%.2f", log.distanceKm)} km",
            durationText = formatDuration(log.durationSeconds),
            avgSpeedText = "${String.format("%.1f", avgSpeed)} km/h",
            paceText = "${log.paceMinPerKm} /km",
            caloriesText = "${max(0, log.caloriesBurned)} kcal",
            raw = log,
            speedData = speedData
        )
    }

    private fun formatDuration(sec: Int): String {
        val m = (sec % 3600) / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }
}