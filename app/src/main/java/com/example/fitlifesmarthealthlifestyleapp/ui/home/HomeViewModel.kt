package com.example.fitlifesmarthealthlifestyleapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val waterRepository = WaterRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _waterLog = MutableLiveData<WaterLog>()
    val waterLog: LiveData<WaterLog> = _waterLog

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage


    fun loadTodayWaterLog() {
        val uid = auth.currentUser?.uid ?: return
        val todayId = DateUtils.getCurrentDateId()

        _isLoading.value = true

        viewModelScope.launch {
            // Lấy log của ngày hôm nay
            val result = waterRepository.getWaterLog(uid, todayId)

            if (result.isSuccess) {
                val existingLog = result.getOrNull()

                if (existingLog != null) {
                    // TH1: Đã có log hôm nay -> Hiển thị lên
                    _waterLog.value = existingLog!!
                } else {
                    // TH2: Chưa có log (Ngày mới) -> TẠO MỚI TỰ ĐỘNG
                    createNewLogForToday(uid, todayId)
                }
            } else {
                _toastMessage.value = "Lỗi tải dữ liệu: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    // Logic tạo log mới (Hybrid Goal)
    private suspend fun createNewLogForToday(uid: String, todayId: String) {
        // 1. Lấy thông tin User để biết cân nặng
        val userResult = userRepository.getUserDetails(uid)
        val user = userResult.getOrNull()

        // 2. Tính toán mục tiêu
        // Nếu user đã từng set cứng mục tiêu (ví dụ lưu trong field preferredGoal), ưu tiên lấy nó
        // Ở đây mình làm đơn giản: Tính theo công thức Weight * 35
        val weight = user?.weight ?: 60f // Mặc định 60kg nếu chưa có
        val calculatedGoal = (weight * 35).toInt()
        // Làm tròn đến hàng trăm (VD: 2135 -> 2100 hoặc 2200)
        val finalGoal = (calculatedGoal / 100) * 100

        // 3. Tạo object mới
        val newLog = WaterLog(
            id = todayId,
            userId = uid,
            currentIntake = 0,
            dailyGoal = finalGoal // <--- Đây là giá trị tự động tính
        )

        // 4. Lưu lên Firestore và cập nhật UI
        waterRepository.saveWaterLog(newLog)
        _waterLog.value = newLog
    }

    // Hàm thêm nước (Uống 1 ly)
    fun addWater(amount: Int) {
        val currentLog = _waterLog.value ?: return

        // Update local UI ngay lập tức cho mượt
        currentLog.currentIntake += amount
        _waterLog.value = currentLog

        // Sync lên Firestore
        viewModelScope.launch {
            waterRepository.saveWaterLog(currentLog)
        }
    }

    // Hàm chỉnh sửa mục tiêu thủ công (Feature 3)
    fun updateDailyGoal(newGoal: Int) {
        val currentLog = _waterLog.value ?: return

        currentLog.dailyGoal = newGoal
        _waterLog.value = currentLog

        viewModelScope.launch {
            waterRepository.saveWaterLog(currentLog)
            _toastMessage.value = "Đã cập nhật mục tiêu!"
        }
    }
}