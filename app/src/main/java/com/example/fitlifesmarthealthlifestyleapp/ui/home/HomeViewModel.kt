package com.example.fitlifesmarthealthlifestyleapp.ui.home

import androidx. lifecycle.LiveData
import androidx. lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.NutritionRepository
import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
import com.example.fitlifesmarthealthlifestyleapp.data. repository.UserRepository
import com. example.fitlifesmarthealthlifestyleapp.data.repository.WaterRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.Event
import com.example.fitlifesmarthealthlifestyleapp.domain. model.User
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.example. fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _totalCalories = MutableLiveData<Int>()
    val totalCalories: LiveData<Int> = _totalCalories

    private val _todaySteps = MutableLiveData<Int>()
    val todaySteps: LiveData<Int> = _todaySteps

    private var stepsStreamJob: Job? = null

    init {
        // Kích hoạt lắng nghe Realtime ngay khi ViewModel được tạo
        listenToUserChanges()
        listenToStepChanges()
    }

    // --- MỚI: Lắng nghe số bước chân hôm nay Real-time ---
    private fun listenToStepChanges() {
        val uid = auth.currentUser?.uid ?: return
        stepsStreamJob?.cancel()
        stepsStreamJob = viewModelScope.launch {
            stepRepository.getTodayStepsStream(uid).collect { steps ->
                _todaySteps.postValue(steps)
            }
        }
    }

    fun loadTodayCalories() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = userRepository.getTodayActiveCalories(uid)

            if (result.isSuccess) {
                val activeCals = result.getOrDefault(0)
                _totalCalories.value = activeCals
            } else {
                _totalCalories.value = 0
            }
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
        viewModelScope.launch {
            stepRepository.incrementSteps(uid, steps)
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
                _toastMessage.value = Event("Lỗi tải dữ liệu:  ${result.exceptionOrNull()?.message}")
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
                _toastMessage.value = Event("Failed to load goals: ${result.exceptionOrNull()?.message}")
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
                updateDailyGoal(waterGoal)
                loadUserGoals()
                _toastMessage.value = Event("toast_goals_saved")
            } else {
                _toastMessage.value = Event("toast_goals_failed")
            }

            _isLoading.value = false
        }
    }


    private suspend fun createNewLogForToday(uid: String, todayId: String) {
        val userResult = userRepository.getUserDetails(uid)
        val user = userResult.getOrNull()

        val finalGoal = if (user != null && user.dailyWaterGoal > 0) {
            user.dailyWaterGoal
        } else {
            val weight = user?.weight ?: 60f
            val activityLevel = user?.activityLevel
            calculateWaterGoal(weight, activityLevel)
        }

        val newLog = WaterLog(
            id = todayId,
            userId = uid,
            currentIntake = 0,
            dailyGoal = finalGoal
        )

        waterRepository.saveWaterLog(newLog)
        _waterLog.value = newLog
    }

    private fun calculateWaterGoal(weight: Float, activityLevel: String?): Int {
        val factor = when (activityLevel) {
            "Sedentary" -> 30
            "Lightly Active" -> 35
            "Moderately Active" -> 40
            "Very Active" -> 45
            else -> 35
        }
        val calculated = weight * factor
        return ((calculated + 50) / 100).toInt() * 100
    }

    fun addWater(amount: Int) {
        val currentLog = _waterLog.value ?: return
        currentLog.currentIntake += amount
        _waterLog.value = currentLog
        viewModelScope.launch {
            waterRepository.saveWaterLog(currentLog)
        }
    }

    fun removeWater(amount: Int) {
        val currentLog = _waterLog.value ?: return
        if (currentLog.currentIntake > 0) {
            currentLog.currentIntake = (currentLog.currentIntake - amount).coerceAtLeast(0)
            _waterLog.value = currentLog
            viewModelScope.launch {
                waterRepository.saveWaterLog(currentLog)
            }
        }
    }

    fun updateDailyGoal(newGoal: Int) {
        val currentLog = _waterLog.value ?: return
        val uid = auth.currentUser?.uid ?: return
        currentLog.dailyGoal = newGoal
        _waterLog.value = currentLog
        viewModelScope.launch {
            waterRepository.saveWaterLog(currentLog)
            userRepository.updateUserWaterGoal(uid, newGoal)
        }
    }

    private fun listenToUserChanges() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserDetailsStream(uid).collect { user ->
                if (user != null) {
                    _user.postValue(user)
                    checkAndAutoUpdateGoal(user)
                }
            }
        }
    }

    private fun checkAndAutoUpdateGoal(user: User) {
        val currentLog = _waterLog.value ?: return
        if (user.dailyWaterGoal > 0) return
        val newAutoGoal = calculateWaterGoal(user.weight, user.activityLevel)
        if (currentLog.dailyGoal != newAutoGoal) {
            currentLog.dailyGoal = newAutoGoal
            _waterLog.value = currentLog
            viewModelScope.launch {
                waterRepository.saveWaterLog(currentLog)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepsStreamJob?.cancel()
    }
}
