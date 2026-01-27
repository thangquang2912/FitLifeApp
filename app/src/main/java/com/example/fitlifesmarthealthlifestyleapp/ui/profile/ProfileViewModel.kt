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

    // LiveData chứa danh sách các thứ trong tuần (Mon, Tue...) cho biểu đồ
    private val _weeklyLabels = MutableLiveData<List<String>>()
    val weeklyLabels: LiveData<List<String>> = _weeklyLabels

    fun fetchUserProfile() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _user.value = null
            return
        }

        if (_user.value == null || _user.value?.uid != currentUid) {
            _isLoading.value = true

            viewModelScope.launch {
                val result = userRepository.getUserDetails(currentUid)

                if (result.isSuccess) {
                    _user.value = result.getOrNull()
                    
                    // Tạo danh sách nhãn ngày (Labels) cho 7 ngày gần nhất
                    generateWeeklyLabels()
                    
                    fetchWeeklyWaterData(currentUid)
                    fetchWeeklyStepsData(currentUid)
                } else {
                    val error = result.exceptionOrNull()
                    error?.printStackTrace()
                    _statusMessage.value = "Lỗi tải dữ liệu: ${result.exceptionOrNull()?.message}"
                }

                _isLoading.value = false
            }
        }
    }

    // Hàm tạo nhãn cho biểu đồ (7 ngày gần nhất tính từ hôm nay ngược về trước)
    private fun generateWeeklyLabels() {
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // VD: Mon, Tue...
        val labels = mutableListOf<String>()
        
        for (i in 0 until 7) {
            labels.add(dayFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        // Đảo ngược để có thứ tự từ cũ đến mới (hôm nay nằm cuối cùng bên phải)
        _weeklyLabels.value = labels.reversed()
    }

    private fun fetchWeeklyWaterData(uid: String) {
        viewModelScope.launch {
            val result = waterRepository.getWeeklyWaterLogs(uid)
            if (result.isSuccess) {
                val logs = result.getOrNull() ?: emptyList()
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
                
                _weeklyWaterLogs.value = last7DaysData
            }
        }
    }

    private fun fetchWeeklyStepsData(uid: String) {
        viewModelScope.launch {
            val result = stepRepository.getWeeklySteps(uid)
            if (result.isSuccess) {
                val dataList = result.getOrNull() ?: emptyList()
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
                
                _weeklySteps.value = last7DaysSteps
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
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
                override fun onStart(requestId: String) { }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as String
                    user.photoUrl = secureUrl
                    updateUserProfile(user)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    _isLoading.postValue(false)
                    _statusMessage.postValue("Lỗi upload ảnh: ${error.description}")
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) { }
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
