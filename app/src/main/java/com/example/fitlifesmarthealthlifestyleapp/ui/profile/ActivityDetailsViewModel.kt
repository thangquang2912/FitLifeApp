package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.ActivityRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ActivityDetailsViewModel : ViewModel() {

    private val repo = ActivityRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _activity = MutableLiveData<ActivityLog?>()
    val activity: LiveData<ActivityLog?> = _activity

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast

    fun load(activityId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _toast.value = "Please login first"
            return
        }

        viewModelScope.launch {
            val result = repo.getActivityLogById(uid, activityId)
            if (result.isSuccess) _activity.value = result.getOrNull()
            else _toast.value = result.exceptionOrNull()?.message ?: "Failed to load activity"
        }
    }

    fun clearToast() { _toast.value = null }
}