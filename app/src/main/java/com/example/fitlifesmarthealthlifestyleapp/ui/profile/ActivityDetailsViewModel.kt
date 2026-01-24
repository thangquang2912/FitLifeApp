package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.ActivityRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

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
    val raw: ActivityLog
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
                _state.value = ActivityDetailsState.Success(toUi(log))
            } else {
                _state.value = ActivityDetailsState.Error(result.exceptionOrNull()?.message ?: "Failed to load activity")
            }
        }
    }

    private fun toUi(log: ActivityLog): ActivityDetailsUi {
        val df = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
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
            raw = log
        )
    }

    private fun formatDuration(sec: Int): String {
        val m = (sec % 3600) / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }
}