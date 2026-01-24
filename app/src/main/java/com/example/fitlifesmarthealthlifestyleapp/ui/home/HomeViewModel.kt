package com.example.fitlifesmarthealthlifestyleapp.ui.home

import androidx. lifecycle.LiveData
import androidx. lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.NutritionRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
import com.example.fitlifesmarthealthlifestyleapp.data. repository.UserRepository
import com. example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.domain. model.User
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.example. fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {
    private val waterRepository = WaterRepository()
    private val userRepository = UserRepository()
    private val nutritionRepository = NutritionRepository()
    private val stepRepository = StepRepository()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "HomeViewModel"

    private val _waterLog = MutableLiveData<WaterLog>()
    val waterLog: LiveData<WaterLog> = _waterLog

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _totalCalories = MutableLiveData<Int>()
    val totalCalories: LiveData<Int> = _totalCalories

    private val _todaySteps = MutableLiveData<Int>()
    val todaySteps: LiveData<Int> = _todaySteps

    init {
        // Kích hoạt lắng nghe Realtime ngay khi ViewModel được tạo
        listenToUserChanges()
    }

    fun loadTodayCalories() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val summary = nutritionRepository.getDailySummary(uid, Date())
            val calories = summary?.get("totalCalories") as? Long ?: 0L
            _totalCalories.value = calories.toInt()
        }
    }

    fun loadTodaySteps() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val steps = stepRepository.getTodaySteps(uid)
            _todaySteps.value = steps
        }
    }

    fun updateSteps(steps: Int) {
        val uid = auth.currentUser?.uid ?: return
        val currentSteps = _todaySteps.value ?: 0
        val newTotalSteps = currentSteps + steps
        _todaySteps.value = newTotalSteps
        
        viewModelScope.launch {
            stepRepository.updateSteps(uid, newTotalSteps)
        }
    }

    fun loadTodayWaterLog() {
        val uid = auth.currentUser?.uid ?: run {
            return
        }
        val todayId = DateUtils.getCurrentDateId()
        _isLoading.value = true

        viewModelScope.launch {
            val result = waterRepository.getWaterLog(uid, todayId)

            if (result.isSuccess) {
                val existingLog = result.getOrNull()

                if (existingLog != null) {
                    _waterLog.value = existingLog
                } else {
                    createNewLogForToday(uid, todayId)
                }
            } else {
                _toastMessage.value = "Lỗi tải dữ liệu:  ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadUserGoals() {
        val uid = auth.currentUser?.uid ?: run {
            return
        }
        _isLoading.value = true

        viewModelScope.launch {
            val result = userRepository.getUserDetails(uid)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _user.value = user
            } else {
                _toastMessage.value = "Failed to load goals: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun saveGoals(
        waterGoal: Int,
        stepsGoal: Int,
        activeCalories: Int,
        caloriesConsume: Int,
        weeklyRunning: Int
    ) {
        val uid = auth.currentUser?.uid ?:  return
        _isLoading.value = true

        viewModelScope.launch {
            val result = userRepository.updateUserGoals(
                uid = uid,
                dailyWaterGoal = waterGoal,
                dailyStepsGoal = stepsGoal,
                dailyActiveCalories = activeCalories,
                dailyCaloriesConsume = caloriesConsume,
                weeklyRunning = weeklyRunning
            )

            if (result.isSuccess) {
                // Cập nhật water log nếu thay đổi water goal
                updateDailyGoal(waterGoal)

                // Reload user data
                loadUserGoals()
                _toastMessage.value = "Goals saved successfully! "
            } else {
                _toastMessage.value = "Failed to save goals: ${result. exceptionOrNull()?.message}"
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
            val activityLevel = user?.activityLevel
            calculateWaterGoal(weight, activityLevel)
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

    // Hàm tính toán lượng nước dựa trên Cân nặng + Activity Level
    private fun calculateWaterGoal(weight: Float, activityLevel: String?): Int {
        val factor = when (activityLevel) {
            "Sedentary" -> 30
            "Lightly Active" -> 35
            "Moderately Active" -> 40
            "Very Active" -> 45
            else -> 35
        }

        val calculated = weight * factor

        // Làm tròn đến hàng trăm
        return ((calculated + 50) / 100).toInt() * 100
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

    // Hàm trừ nước
    fun removeWater(amount: Int) {
        val currentLog = _waterLog.value ?: return

        // Kiểm tra nếu lượng nước hiện tại > 0 thì mới trừ
        if (currentLog.currentIntake > 0) {
            // Sử dụng coerceAtLeast(0) để đảm bảo không bao giờ bị âm
            currentLog.currentIntake = (currentLog.currentIntake - amount).coerceAtLeast(0)
            _waterLog.value = currentLog

            // Cập nhật lên Firestore
            viewModelScope.launch {
                waterRepository.saveWaterLog(currentLog)
            }
        } else {
            _toastMessage.value = "Lượng nước đã về 0 rồi!"
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

        // 2. Nếu User để Auto -> Tính lại theo cân nặng + mức độ vận động mới nhất vừa nhận được
        val newAutoGoal = calculateWaterGoal(user.weight, user.activityLevel)

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
