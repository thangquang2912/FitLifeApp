package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NutritionFragment : Fragment() {

    private lateinit var nutritionViewModel: NutritionViewModel

    // Views
    private lateinit var tvTotalMacro: TextView
    private lateinit var tvCarbValue: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvFatValue: TextView
    private lateinit var macroRingView: MacroRingView
    private lateinit var tvCalorieValue: TextView
    private lateinit var tvMealsCount: TextView
    private lateinit var btnAddMeal: ImageButton
    private lateinit var tvRemainingValue: TextView
    private lateinit var tvDateSelector: TextView

    // AI Views
    private lateinit var cardAiInsights: CardView
    private lateinit var layoutAiInvite: LinearLayout
    private lateinit var layoutAiLoading: LinearLayout
    private lateinit var layoutAiContent: LinearLayout
    private lateinit var btnAskAi: View
    private lateinit var tvAiDesc: TextView
    private lateinit var tvSuggestionIcon: TextView
    private lateinit var tvSuggestionName: TextView
    private lateinit var tvSuggestionDetail: TextView
    private lateinit var btnRefreshAi: TextView
    private lateinit var cardMacros: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nutrition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nutritionViewModel = ViewModelProvider(this)[NutritionViewModel::class.java]

        initViews(view)
        setupListeners()
        observeViewModel()

        nutritionViewModel.loadData()
    }

    private fun initViews(view: View) {
        tvTotalMacro = view.findViewById(R.id.tvTotalMacro)
        tvCarbValue = view.findViewById(R.id.tvCarbValue)
        tvProteinValue = view.findViewById(R.id.tvProteinValue)
        tvFatValue = view.findViewById(R.id.tvFatValue)
        macroRingView = view.findViewById(R.id.macroRingView)
        btnAddMeal = view.findViewById(R.id.btnAddMeal)
        tvCalorieValue = view.findViewById(R.id.tvCalorieValue)
        tvMealsCount = view.findViewById(R.id.tvMealsValue)
        tvRemainingValue = view.findViewById(R.id.tvRemainingValue)
        tvDateSelector = view.findViewById(R.id.tvDateSelector)
        cardMacros = view.findViewById(R.id.cardMacros)

        // Ánh xạ AI Views
        cardAiInsights = view.findViewById(R.id.cardAiInsights)

        layoutAiInvite = view.findViewById(R.id.layoutAiInvite)
        layoutAiLoading = view.findViewById(R.id.layoutAiLoading)
        layoutAiContent = view.findViewById(R.id.layoutAiContent)

        btnAskAi = view.findViewById(R.id.btnAskAi)
        btnRefreshAi = view.findViewById(R.id.btnRefreshAi)

        tvAiDesc = view.findViewById(R.id.tvAiDesc)
        tvSuggestionIcon = view.findViewById(R.id.tvSuggestionIcon)
        tvSuggestionName = view.findViewById(R.id.tvSuggestionName)
        tvSuggestionDetail = view.findViewById(R.id.tvSuggestionDetail)
    }

    private fun setupListeners() {
        btnAddMeal.setOnClickListener {
            val dialog = AddMealDialogFragment { name, calories, carb, pro, fat, portion, imgUri ->
                nutritionViewModel.addMeal(name, calories, carb, pro, fat, portion, imgUri)
            }
            dialog.show(parentFragmentManager, "AddMealDialog")
        }

        tvDateSelector.setOnClickListener { showDatePicker() }

        // SỰ KIỆN NÚT HỎI AI (Lần đầu)
        btnAskAi.setOnClickListener {
            performAiRequest()
        }

        // --- BỔ SUNG: SỰ KIỆN NÚT REFRESH (Thử món khác) ---
        btnRefreshAi.setOnClickListener {
            performAiRequest()
        }

        cardMacros.setOnClickListener {
            // Lấy danh sách món ăn hiện tại từ ViewModel
            val currentMeals = nutritionViewModel.meals.value ?: emptyList()

            // Mở BottomSheet
            val bottomSheet = MealHistoryBottomSheet(currentMeals)
            bottomSheet.show(parentFragmentManager, "MealHistoryBottomSheet")
        }
    }

    // Hàm gọi AI dùng chung cho cả 2 nút
    private fun performAiRequest() {
        // 1. Chuyển sang Loading
        layoutAiInvite.visibility = View.GONE
        layoutAiContent.visibility = View.GONE
        layoutAiLoading.visibility = View.VISIBLE

        // 2. Gọi ViewModel (Hàm này sẽ tự động ghi đè cache cũ)
        nutritionViewModel.generateAiSuggestion()
    }

    private fun observeViewModel() {
        nutritionViewModel.nutritionSummary.observe(viewLifecycleOwner) { summary ->
            tvCalorieValue.text = summary.totalCalories.toString()
            tvMealsCount.text = summary.mealsCount.toString()
            tvCarbValue.text = "${summary.totalCarbs.toInt()}g"
            tvProteinValue.text = "${summary.totalProtein.toInt()}g"
            tvFatValue.text = "${summary.totalFat.toInt()}g"

            val totalGrams = (summary.totalCarbs + summary.totalProtein + summary.totalFat).toInt()
            tvTotalMacro.text = "${totalGrams}g"

            val goal = nutritionViewModel.calorieGoal.value ?: 2000
            val remaining = goal - summary.totalCalories
            tvRemainingValue.text = if (remaining < 0) "0" else remaining.toString()

            macroRingView.setMacros(summary.totalCarbs, summary.totalProtein, summary.totalFat, goal)
        }

        nutritionViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            tvDateSelector.text = formatDateDisplay(date)
        }

        nutritionViewModel.aiSuggestion.observe(viewLifecycleOwner) { suggestion ->
            if (suggestion != null) {
                // A. CÓ KẾT QUẢ -> Hiện Content
                layoutAiInvite.visibility = View.GONE
                layoutAiLoading.visibility = View.GONE
                layoutAiContent.visibility = View.VISIBLE

                tvAiDesc.text = suggestion.reason
                tvSuggestionIcon.text = suggestion.icon
                tvSuggestionName.text = suggestion.dishName
                tvSuggestionDetail.text = "${suggestion.protein}g protein • ${suggestion.calories} calories"
            } else {
                // B. NULL -> Hiện nút Invite (nếu không phải đang loading)
                if (layoutAiLoading.visibility != View.VISIBLE) {
                    layoutAiInvite.visibility = View.VISIBLE
                    layoutAiLoading.visibility = View.GONE
                    layoutAiContent.visibility = View.GONE
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        nutritionViewModel.selectedDate.value?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                nutritionViewModel.changeDate(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun formatDateDisplay(date: Date): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance()
        target.time = date
        return when {
            android.text.format.DateUtils.isToday(date.time) -> "Today, " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}