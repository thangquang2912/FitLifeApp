package com.example.fitlifesmarthealthlifestyleapp.ui.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class HomeFragment : Fragment() {

    private lateinit var tvGreeting : TextView
    private lateinit var tvDate : TextView
    private lateinit var cardSteps : View
    private lateinit var stepsIcon : ImageView
    private lateinit var stepsValue : TextView
    private lateinit var stepsLabel : TextView
    private lateinit var cardCalories : View
    private lateinit var caloriesIcon : ImageView
    private lateinit var caloriesValue : TextView
    private lateinit var caloriesLabel : TextView
    private lateinit var cardWater : View
    private lateinit var waterIcon : ImageView
    private lateinit var waterValue : TextView
    private lateinit var waterLabel : TextView

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tvIntakeProgress : TextView
    private lateinit var progressBarWater : ProgressBar
    private lateinit var layoutDropsContainer : LinearLayout
    private lateinit var imgWaterIcon : ImageView

    private lateinit var btnStart : MaterialButton
    private lateinit var btnSetGoals : MaterialButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        initViews(view)

        setupHeader()
        setupDashboard()
        setupButtonListeners()

        observeViewModel()
        imgWaterIcon.setOnClickListener {
            homeViewModel.addWater(250)
        }


        homeViewModel.loadTodayWaterLog()
    }

    private fun initViews(view:View) {
        tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        tvDate = view.findViewById<TextView>(R.id.tvDate)

        cardSteps = view.findViewById<View>(R.id.cardSteps)
        stepsIcon = cardSteps.findViewById<ImageView>(R.id.imgStatIcon)
        stepsValue = cardSteps.findViewById<TextView>(R.id.tvStatValue)
        stepsLabel = cardSteps.findViewById<TextView>(R.id.tvStatLabel)

        cardCalories = view.findViewById<View>(R.id.cardCalories)
        caloriesIcon = cardCalories.findViewById<ImageView>(R.id.imgStatIcon)
        caloriesValue = cardCalories.findViewById<TextView>(R.id.tvStatValue)
        caloriesLabel = cardCalories.findViewById<TextView>(R.id.tvStatLabel)

        cardWater = view.findViewById<View>(R.id.cardWater)
        waterIcon = cardWater.findViewById<ImageView>(R.id.imgStatIcon)
        waterValue = cardWater.findViewById<TextView>(R.id.tvStatValue)
        waterLabel = cardWater.findViewById<TextView>(R.id.tvStatLabel)

        tvIntakeProgress = view.findViewById<TextView>(R.id.tvIntakeProgress)
        progressBarWater = view.findViewById<ProgressBar>(R.id.progressBarWater)
        layoutDropsContainer = view.findViewById<LinearLayout>(R.id.layoutDropsContainer)
        imgWaterIcon = view.findViewById<ImageView>(R.id.imgWaterIcon)

        btnStart = view.findViewById(R.id.btnStart)
        btnSetGoals = view.findViewById(R.id.btnSetGoals)
    }

    private fun setupHeader() {
        // --- Xử lý Lời chào theo giờ ---
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greetingText = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
        tvGreeting.text = "$greetingText! 👋"

        // --- Xử lý Ngày tháng ---
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        tvDate.text = dateFormat.format(Date())
    }

    private fun setupDashboard() {
        // 1. Steps
        stepsIcon.setImageResource(R.drawable.ic_steps)
        stepsValue.text = "8,234"
        stepsLabel.text = "Steps"

        // 2. Calories
        caloriesIcon.setImageResource(R.drawable.ic_calories)
        caloriesValue.text = "1,847"
        caloriesLabel.text = "Calories"

        // 3. Water
        waterIcon.setImageResource(R.drawable.ic_stat_water)
//        waterValue.text = "6"
        waterLabel.text = "Water"
    }

    private fun setupButtonListeners() {

        btnStart.setOnClickListener {
            navigateToActivityTracking()
        }

        // Click Set Goals -> Hiển thị Dialog
        btnSetGoals.setOnClickListener {
            showSetGoalsDialog()
        }
    }

    private fun navigateToActivityTracking() {
        try {
            // Tìm BottomNavigationView từ Activity
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(
                R.id.bottom_nav // ID thực tế của bottom nav trong MainFragment/MainActivity
            )

            bottomNav?.selectedItemId = R.id.activity // ID của menu item Activity

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Cannot navigate to Activity tab",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSetGoalsDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_set_goals)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Initialize dialog views
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val btnSaveGoals = dialog.findViewById<MaterialButton>(R.id.btnSaveGoals)

        // Steps controls
        val tvStepsValue = dialog.findViewById<TextView>(R.id.tvStepsValue)
        val tvStepsTarget = dialog.findViewById<TextView>(R.id.tvStepsTarget)
        val sliderSteps = dialog.findViewById<Slider>(R.id.sliderSteps)
        val progressSteps = dialog.findViewById<ProgressBar>(R.id.progressSteps)
        val btnIncreaseSteps = dialog.findViewById<ImageView>(R.id.btnIncreaseSteps)
        val btnDecreaseSteps = dialog.findViewById<ImageView>(R.id.btnDecreaseSteps)

        // Water controls
        val tvWaterValue = dialog.findViewById<TextView>(R.id.tvWaterValue)
        val tvWaterTarget = dialog.findViewById<TextView>(R.id.tvWaterTarget)
        val sliderWater = dialog.findViewById<Slider>(R.id.sliderWater)
        val progressWater = dialog.findViewById<ProgressBar>(R.id.progressWater)
        val btnIncreaseWater = dialog.findViewById<ImageView>(R.id.btnIncreaseWater)
        val btnDecreaseWater = dialog.findViewById<ImageView>(R.id.btnDecreaseWater)

        // Initial values
        var stepsGoal = 10000
        var waterGoal = 2000

        // Setup Steps Slider
        sliderSteps.addOnChangeListener { _, value, _ ->
            stepsGoal = value.toInt()
            tvStepsValue.text = String.format("%,d", stepsGoal)
            tvStepsTarget.text = "Target: ${String.format("%,d", stepsGoal)} steps"

            // Update progress (giả sử current = 8000)
            val currentSteps = 8000
            val progress = (currentSteps.toFloat() / stepsGoal * 100).toInt()
            progressSteps.progress = progress.coerceIn(0, 100)
        }

        // Steps increase/decrease buttons
        btnIncreaseSteps.setOnClickListener {
            val newValue = (sliderSteps.value + 1000).coerceAtMost(sliderSteps.valueTo)
            sliderSteps.value = newValue
        }

        btnDecreaseSteps.setOnClickListener {
            val newValue = (sliderSteps.value - 1000).coerceAtLeast(sliderSteps.valueFrom)
            sliderSteps.value = newValue
        }

        // Setup Water Slider
        sliderWater.addOnChangeListener { _, value, _ ->
            waterGoal = value.toInt()
            tvWaterValue.text = String.format("%,d", waterGoal)
            tvWaterTarget.text = "Target: ${String.format("%,d", waterGoal)} ml"

            // Update progress (giả sử current = 1250)
            val currentWater = 1250
            val progress = (currentWater.toFloat() / waterGoal * 100).toInt()
            progressWater.progress = progress.coerceIn(0, 100)
        }

        // Water increase/decrease buttons
        btnIncreaseWater.setOnClickListener {
            val newValue = (sliderWater.value + 250).coerceAtMost(sliderWater.valueTo)
            sliderWater.value = newValue
        }

        btnDecreaseWater.setOnClickListener {
            val newValue = (sliderWater.value - 250).coerceAtLeast(sliderWater.valueFrom)
            sliderWater.value = newValue
        }

        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Save button
        btnSaveGoals.setOnClickListener {
            // TODO: Save goals to ViewModel/Database
            Toast.makeText(
                requireContext(),
                "Goals saved! Steps: $stepsGoal, Water: ${waterGoal}ml",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun observeViewModel() {
        // Khi data WaterLog thay đổi -> Cập nhật UI
        homeViewModel.waterLog.observe(viewLifecycleOwner) { log ->
            if (log != null) {
                updateWaterUI(log)
            }
        }

        // Khi có thông báo lỗi hoặc thành công (Toast)
        homeViewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // Khi đang loading (có thể hiện ProgressBar xoay xoay nếu muốn)
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // if (isLoading) showLoading() else hideLoading()
        }
    }

    private fun updateWaterUI(log: WaterLog) {
        // --- 1. Cập nhật Thẻ Top (Số ly) ---
        val glassCount = log.getGlassCount()
        waterValue.text = glassCount.toString()

        // --- 2. Cập nhật Thẻ Chi tiết (Bottom Card) ---

        // Text: 1250ml / 2000ml
        tvIntakeProgress.text = "${log.currentIntake}ml / ${log.dailyGoal}ml"

        // Progress Bar
        progressBarWater.max = log.dailyGoal
        progressBarWater.progress = log.currentIntake

        // Render Giọt nước (Drops)
        renderWaterDrops(currentCups = glassCount, totalCups = log.dailyGoal / 250)
    }

    private fun renderWaterDrops(currentCups: Int, totalCups: Int) {
        layoutDropsContainer.removeAllViews()

        // Kích thước mỗi giọt nước (tùy chỉnh cho vừa UI)
        val iconSize = 80
        val margin = 8

        for (i in 0 until totalCups) {
            val dropIcon = ImageView(requireContext())
            val params = LinearLayout.LayoutParams(iconSize, iconSize)
            params.setMargins(margin, 0, margin, 0)
            dropIcon.layoutParams = params

            if (i < currentCups) {
                dropIcon.setImageResource(R.drawable.ic_water)
            } else {
                dropIcon.setImageResource(R.drawable.ic_water)
                dropIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray_text))
            }
            layoutDropsContainer.addView(dropIcon)
        }
    }
}

