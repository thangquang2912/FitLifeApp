package com.example.fitlifesmarthealthlifestyleapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
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


    init {
        // Kích hoạt lắng nghe Realtime ngay khi ViewModel được tạo
        listenToUserChanges()
    }

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
        val finalGoal = if (user != null && user.dailyWaterGoal > 0) {
            // TH1: User đã từng tự set goal (ví dụ 3000ml) -> Dùng luôn số đó
            user.dailyWaterGoal
        } else {
            // TH2: User chưa set hoặc reset -> Tính theo cân nặng (Weight * 35)
            val weight = user?.weight ?: 60f
            val calculated = (weight * 35).toInt()
            (calculated / 100) * 100 // Làm tròn
        }

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
        val uid = auth.currentUser?.uid ?: return

        // Update 1: Cập nhật UI và Log của ngày hôm nay ngay lập tức
        currentLog.dailyGoal = newGoal
        _waterLog.value = currentLog

        viewModelScope.launch {
            // Lưu log hôm nay xuống Firestore (WaterLog collection)
            waterRepository.saveWaterLog(currentLog)

            // Update 2: Cập nhật vào User Profile
            userRepository.updateUserWaterGoal(uid, newGoal)

            _toastMessage.value = "Daily goal updated!"
        }
    }

    private fun listenToUserChanges() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // Lắng nghe liên tục
            userRepository.getUserDetailsStream(uid).collect { user ->
                if (user != null) {
                    // Khi có User mới (do thay đổi cân nặng, goal...), ta check logic ngay
                    checkAndAutoUpdateGoal(user)
                }
            }
        }
    }

    // Hàm logic tự động cập nhật Goal
    private fun checkAndAutoUpdateGoal(user: User) {
        val currentLog = _waterLog.value ?: return // Nếu chưa load được log hôm nay thì bỏ qua

        // 1. Nếu User đã set cứng mục tiêu -> Không làm gì cả
        if (user.dailyWaterGoal > 0) return

        // 2. Nếu User để Auto -> Tính lại theo cân nặng mới nhất vừa nhận được
        val weight = user.weight
        val newAutoGoal = ((weight * 35).toInt() / 100) * 100

        // 3. So sánh: Nếu khác với Goal hiện tại trong Log thì mới update
        if (currentLog.dailyGoal != newAutoGoal) {
            currentLog.dailyGoal = newAutoGoal

            // Cập nhật UI ngay lập tức
            _waterLog.value = currentLog

            // Lưu xuống Firestore (update lại snapshot log hôm nay)
            viewModelScope.launch {
                waterRepository.saveWaterLog(currentLog)
            }
        }
    }
}