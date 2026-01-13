package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.NutritionRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Meal
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NutritionRepository()
    private val auth = FirebaseAuth.getInstance()
    private val geminiHelper = GeminiNutritionHelper()
    private var todaySuggestionCache: GeminiNutritionHelper.MealSuggestion? = null

    // 1. Quản lý ngày đang chọn
    private val _selectedDate = MutableLiveData<Date>(Date())
    val selectedDate: LiveData<Date> = _selectedDate

    // 2. LiveData cũ
    private val _meals = MutableLiveData<List<Meal>>()
    val meals: LiveData<List<Meal>> = _meals

    data class NutritionSummary(
        val totalCalories: Int = 0,
        val totalCarbs: Float = 0f,
        val totalProtein: Float = 0f,
        val totalFat: Float = 0f,
        val mealsCount: Int = 0
    )

    private val _nutritionSummary = MutableLiveData<NutritionSummary>()
    val nutritionSummary: LiveData<NutritionSummary> = _nutritionSummary

    private val _calorieGoal = MutableLiveData<Int>()
    val calorieGoal: LiveData<Int> = _calorieGoal

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData cho Gợi ý AI
    private val _aiSuggestion = MutableLiveData<GeminiNutritionHelper.MealSuggestion?>()
    val aiSuggestion: LiveData<GeminiNutritionHelper.MealSuggestion?> = _aiSuggestion


    fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        val dateToLoad = _selectedDate.value ?: Date()

        viewModelScope.launch {
            val goal = 2000
            _calorieGoal.value = goal

            // A. Lấy Summary
            val summaryData = repository.getDailySummary(uid, dateToLoad)
            if (summaryData != null) {
                val summary = NutritionSummary(
                    totalCalories = (summaryData["totalCalories"] as? Long)?.toInt() ?: 0,
                    totalCarbs = (summaryData["totalCarbs"] as? Double)?.toFloat() ?: 0f,
                    totalProtein = (summaryData["totalProtein"] as? Double)?.toFloat() ?: 0f,
                    totalFat = (summaryData["totalFat"] as? Double)?.toFloat() ?: 0f,
                    mealsCount = (summaryData["mealsCount"] as? Long)?.toInt() ?: 0
                )
                _nutritionSummary.value = summary
            } else {
                _nutritionSummary.value = NutritionSummary()
            }

            // B. Lấy List Meals
            val resultMeals = repository.getMealsByDate(uid, dateToLoad)
            if (resultMeals.isSuccess) {
                _meals.value = resultMeals.getOrDefault(emptyList())
            }
        }
    }

    fun generateAiSuggestion() {
        val summary = _nutritionSummary.value ?: NutritionSummary()
        val goal = _calorieGoal.value ?: 2000

        // Chỉ gợi ý cho ngày hôm nay
        val isToday = android.text.format.DateUtils.isToday(_selectedDate.value?.time ?: 0)
        if (!isToday) {
            _aiSuggestion.value = null
            return
        }

        // 1. Tính Macros còn thiếu
        val goalProtein = (goal * 0.3 / 4).toInt()
        val goalCarb = (goal * 0.5 / 4).toInt()
        val goalFat = (goal * 0.2 / 9).toInt()

        val remCarb = (goalCarb - summary.totalCarbs.toInt()).coerceAtLeast(0)
        val remPro = (goalProtein - summary.totalProtein.toInt()).coerceAtLeast(0)
        val remFat = (goalFat - summary.totalFat.toInt()).coerceAtLeast(0)

        // 2. Xác định thời gian
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when(hour) {
            in 5..10 -> "Morning"
            in 11..14 -> "Lunch"
            in 15..18 -> "Afternoon"
            in 19..22 -> "Dinner"
            else -> "Late Night"
        }

        // 3. Gọi trực tiếp API (Không check Cache nữa)
        viewModelScope.launch {
            // Có thể reset suggestion về null hoặc giữ nguyên để UI hiển thị loading
            // _aiSuggestion.value = null

            val suggestion = geminiHelper.suggestNextMeal(remCarb, remPro, remFat, timeOfDay)

            // Cập nhật kết quả mới (nếu API lỗi thì trả về null, Fragment tự xử lý giữ nguyên UI cũ hoặc báo lỗi)
            if (suggestion != null) {
                _aiSuggestion.value = suggestion
                todaySuggestionCache = suggestion
            } else {
                _toastMessage.value = "AI đang bận, vui lòng thử lại sau!"
                _aiSuggestion.value = null
            }
        }
    }

    fun changeDate(newDate: Date) {
        _selectedDate.value = newDate

        if (isToday(newDate)) {
            _aiSuggestion.value = todaySuggestionCache
        } else {
            _aiSuggestion.value = null
        }
        loadData()
    }

    fun addMeal(name: String, calories: Int, carb: Float, pro: Float, fat: Float, portion: Float, imageUriString: String?) {
        val uid = auth.currentUser?.uid ?: return

        _isLoading.value = true
        _toastMessage.value = "Saving meal..."

        viewModelScope.launch {
            var finalImageUrl: String? = null
            if (imageUriString != null) {
                val uri = Uri.parse(imageUriString)
                finalImageUrl = CloudinaryHelper.uploadImage(uri)
                if (finalImageUrl == null) _toastMessage.value = "Image upload failed. Saving without image."
            }

            val newMeal = Meal(
                id = UUID.randomUUID().toString(),
                userId = uid,
                name = name,
                calories = calories,
                carbs = carb,
                protein = pro,
                fat = fat,
                portion = portion,
                imageUrl = finalImageUrl,
                timestamp = Date()
            )

            val result = repository.addMeal(newMeal)

            if (result.isSuccess) {
                _toastMessage.value = "Added successfully!"
                todaySuggestionCache = null
                changeDate(Date())
            } else {
                _toastMessage.value = "Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance()
        target.time = date

        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }


}