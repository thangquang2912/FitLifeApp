package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.ActivityRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel : ViewModel() {

    private val repo = ActivityRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _items = MutableLiveData<List<ActivityLog>>(emptyList())
    val items: LiveData<List<ActivityLog>> = _items

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast

    fun loadHistory(limit: Int = 100) {
        val uid = auth.currentUser?.uid ?: run {
            _toast.value = "Please login first"
            return
        }

        viewModelScope.launch {
            val result = repo.getActivityLogs(uid, limit)
            if (result.isSuccess) _items.value = result.getOrNull().orEmpty()
            else _toast.value = result.exceptionOrNull()?.message ?: "Failed to load history"
        }
    }

    fun clearToast() { _toast.value = null }
}