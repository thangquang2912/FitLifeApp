package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val waterRepository = WaterRepository()
    private val stepRepository = StepRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _weeklyWaterLogs = MutableLiveData<List<Int>>()
    val weeklyWaterLogs: LiveData<List<Int>> = _weeklyWaterLogs

    private val _weeklySteps = MutableLiveData<List<Int>>()
    val weeklySteps: LiveData<List<Int>> = _weeklySteps

    private val _weeklyLabels = MutableLiveData<List<String>>()
    val weeklyLabels: LiveData<List<String>> = _weeklyLabels

    private var waterStreamJob: Job? = null
    private var stepsStreamJob: Job? = null
    private var userStreamJob: Job? = null

    fun fetchUserProfile() {
        val currentUid = auth.currentUser?.uid ?: return

        // --- CHỈNH SỬA: Lắng nghe User Real-time để cập nhật Goal ngay lập tức ---
        userStreamJob?.cancel()
        userStreamJob = viewModelScope.launch {
            userRepository.getUserDetailsStream(currentUid).collect { user ->
                _user.value = user
                if (user != null) {
                    generateWeeklyLabels()
                    fetchWeeklyWaterData(currentUid)
                    fetchWeeklyStepsData(currentUid)
                }
            }
        }
    }

    private fun generateWeeklyLabels() {
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val labels = mutableListOf<String>()
        for (i in 0 until 7) {
            labels.add(dayFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        _weeklyLabels.value = labels.reversed()
    }

    private fun fetchWeeklyWaterData(uid: String) {
        waterStreamJob?.cancel()
        waterStreamJob = viewModelScope.launch {
            waterRepository.getWeeklyWaterLogsStream(uid).collect { logs ->
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val last7DaysData = mutableListOf<Int>()
                val logMap = logs.associateBy { it.id }
                val tempDays = mutableListOf<String>()
                for (i in 0 until 7) {
                    tempDays.add(sdf.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                tempDays.reversed().forEach { dateId ->
                    last7DaysData.add(logMap[dateId]?.currentIntake ?: 0)
                }
                _weeklyWaterLogs.postValue(last7DaysData)
            }
        }
    }

    private fun fetchWeeklyStepsData(uid: String) {
        stepsStreamJob?.cancel()
        stepsStreamJob = viewModelScope.launch {
            stepRepository.getWeeklyStepsStream(uid).collect { dataList ->
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val last7DaysSteps = mutableListOf<Int>()
                val stepsMap = dataList.associate { 
                    (it["dateId"] as? String ?: "") to (it["steps"] as? Long ?: 0L).toInt()
                }
                val tempDays = mutableListOf<String>()
                for (i in 0 until 7) {
                    tempDays.add(sdf.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                tempDays.reversed().forEach { dateId ->
                    last7DaysSteps.add(stepsMap[dateId] ?: 0)
                }
                _weeklySteps.postValue(last7DaysSteps)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        waterStreamJob?.cancel()
        stepsStreamJob?.cancel()
        userStreamJob?.cancel()
    }

    fun saveUserProfile(user: User, imageUri: Uri?) {
        _isLoading.value = true
        if (imageUri == null) {
            updateUserProfile(user)
            return
        }
        MediaManager.get().upload(imageUri)
            .option("folder", "fitlife_avatars")
            .option("public_id", user.uid)
            .option("overwrite", true)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as String
                    user.photoUrl = secureUrl
                    updateUserProfile(user)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    _isLoading.postValue(false)
                    _statusMessage.postValue("Lỗi upload ảnh: ${error.description}")
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            val result = userRepository.updateUser(user)
            if (result.isSuccess) {
                _user.value = user
                _statusMessage.value = "Updated profile successfully!"
            } else {
                _statusMessage.value = "Lỗi lưu dữ liệu: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
