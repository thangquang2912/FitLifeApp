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
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _weeklyWaterLogs = MutableLiveData<List<Int>>()
    val weeklyWaterLogs: LiveData<List<Int>> = _weeklyWaterLogs

    fun fetchUserProfile() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            _user.value = null
            return
        }

        if (_user.value == null || _user.value?.uid != currentUid) {
            _isLoading.value = true;

            viewModelScope.launch {
                val result = userRepository.getUserDetails(currentUid)

                if (result.isSuccess) {
                    _user.value = result.getOrNull()
                    fetchWeeklyWaterData(currentUid)
                } else {
                    val error = result.exceptionOrNull()
                    error?.printStackTrace()
                    _statusMessage.value = "Lỗi tải dữ liệu: ${result.exceptionOrNull()?.message}"
                }

                _isLoading.value = false
            }
        }
    }

    private fun fetchWeeklyWaterData(uid: String) {
        viewModelScope.launch {
            val result = waterRepository.getWeeklyWaterLogs(uid)
            if (result.isSuccess) {
                val logs = result.getOrNull() ?: emptyList()
                
                // Chuẩn bị dữ liệu cho 7 ngày gần nhất (đảm bảo đủ 7 cột kể cả khi thiếu data Firestore)
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val last7DaysData = mutableListOf<Int>()
                
                val logMap = logs.associateBy { it.id }
                
                // Lấy data ngược từ hôm nay về 6 ngày trước
                val tempDays = mutableListOf<String>()
                for (i in 0 until 7) {
                    tempDays.add(sdf.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                
                // Đảo ngược lại để chart hiện từ cũ -> mới
                tempDays.reversed().forEach { dateId ->
                    last7DaysData.add(logMap[dateId]?.currentIntake ?: 0)
                }
                
                _weeklyWaterLogs.value = last7DaysData
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
    }

    fun saveUserProfile(user: User, imageUri: Uri?) {
        _isLoading.value = true

        // TH1: Không đổi ảnh -> Chỉ lưu thông tin text
        if (imageUri == null) {
            updateUserProfile(user)
            return
        }

        // TH2: Có ảnh -> Upload lên Cloudinary
        MediaManager.get().upload(imageUri)
            .option("folder", "fitlife_avatars")
            .option("public_id", user.uid)
            .option("overwrite", true)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) { }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // 1. Lấy link ảnh HTTPS
                    val secureUrl = resultData["secure_url"] as String

                    // 2. Cập nhật link vào User
                    user.photoUrl = secureUrl

                    // 3. Lưu User vào Firestore
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
                _statusMessage.value = "Updated profile successfully!" // Báo cho Fragment biết
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