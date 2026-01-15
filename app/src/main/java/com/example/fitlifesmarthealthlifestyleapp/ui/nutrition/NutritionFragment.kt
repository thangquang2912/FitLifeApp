package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R

class NutritionFragment : Fragment() {

    private lateinit var nutritionViewModel: NutritionViewModel

    // Khai báo các View trong layout fragment_nutrition.xml
    private lateinit var tvTotalMacro: TextView
    private lateinit var tvCarbValue: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvFatValue: TextView

    // Progress Bars (Vòng tròn biểu đồ)
    private lateinit var progressRingMain: ProgressBar

    // Cards Summary (3 thẻ dưới cùng)
    private lateinit var tvCalorieValue: TextView
    private lateinit var tvMealsCount: TextView

    private lateinit var btnAddMeal: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nutrition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Khởi tạo ViewModel
        nutritionViewModel = ViewModelProvider(this)[NutritionViewModel::class.java]

        // 2. Ánh xạ View
        initViews(view)

        // 3. Lắng nghe sự kiện click
        setupListeners()

        // 4. Quan sát dữ liệu từ ViewModel để cập nhật UI
        observeViewModel()

        // 5. Load dữ liệu ban đầu
        nutritionViewModel.loadData()
    }

    private fun initViews(view: View) {
        tvTotalMacro = view.findViewById(R.id.tvTotalMacro)

        // Tìm các TextView trong phần Legend (Chú thích)
        tvCarbValue = view.findViewById(R.id.tvCarbValue)
        tvProteinValue = view.findViewById(R.id.tvProteinValue)
        tvFatValue = view.findViewById(R.id.tvFatValue)

        progressRingMain = view.findViewById(R.id.progressRingMain)
        btnAddMeal = view.findViewById(R.id.btnAddMeal)

        // Ánh xạ các TextView trong 3 thẻ bottom (sử dụng include id)
        // Lưu ý: Vì dùng <include>, ta cần tìm view cha trước hoặc tìm thẳng ID nếu ID là duy nhất
        val cardCalories = view.findViewById<View>(R.id.cardSummaryCalories)
        tvCalorieValue = cardCalories.findViewById(R.id.tvValue) // ID bên trong item_nutrition_summary
        // Set label thủ công nếu cần, hoặc set cứng trong XML
        cardCalories.findViewById<TextView>(R.id.tvLabel).text = "Calories"

        val cardMeals = view.findViewById<View>(R.id.cardSummaryMeals)
        tvMealsCount = cardMeals.findViewById(R.id.tvValue)
        cardMeals.findViewById<TextView>(R.id.tvLabel).text = "Meals"

        val cardRemaining = view.findViewById<View>(R.id.cardSummaryRemaining)
        cardRemaining.findViewById<TextView>(R.id.tvValue).text = "1,355" // Fake data remaining
        cardRemaining.findViewById<TextView>(R.id.tvLabel).text = "Remaining"
    }

    private fun setupListeners() {
        // Mở Dialog Add Meal khi bấm nút +
        btnAddMeal.setOnClickListener {
            val dialog = AddMealDialogFragment { name, calories, carb, pro, fat ->
                // Callback: Khi bấm Save ở Dialog, code này sẽ chạy
                nutritionViewModel.addMeal(name, calories, carb, pro, fat)
            }
            dialog.show(parentFragmentManager, "AddMealDialog")
        }
    }

    private fun observeViewModel() {
        // Cập nhật UI khi dữ liệu dinh dưỡng thay đổi
        nutritionViewModel.nutritionSummary.observe(viewLifecycleOwner) { summary ->
            // Update Text
            tvCalorieValue.text = summary.totalCalories.toString()

            // Format số float thành chuỗi gọn (ví dụ 105.0 -> 105g)
            tvCarbValue.text = "${summary.totalCarbs.toInt()}g"
            tvProteinValue.text = "${summary.totalProtein.toInt()}g"
            tvFatValue.text = "${summary.totalFat.toInt()}g"

            // Update Tổng Macro ở giữa vòng tròn (Ví dụ tổng gam các chất)
            val totalGrams = (summary.totalCarbs + summary.totalProtein + summary.totalFat).toInt()
            tvTotalMacro.text = "${totalGrams}g"

            // Update Progress Bar (Ví dụ: Calo hiện tại so với Goal 2000)
            // progressRingMain.progress = (summary.totalCalories * 100 / 2000)

            // Hoặc update theo logic Macro (ở đây demo set cứng hoặc theo logic bạn muốn)
            progressRingMain.progress = 65
        }

        nutritionViewModel.meals.observe(viewLifecycleOwner) { meals ->
            tvMealsCount.text = meals.size.toString()
        }

        nutritionViewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}