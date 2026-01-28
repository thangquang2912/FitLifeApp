package com.example.fitlifesmarthealthlifestyleapp.ui.home

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import androidx.fragment.app.Fragment
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
import com.example.fitlifesmarthealthlifestyleapp.domain.service.StepSensorManager
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
    private lateinit var gestureDetector: GestureDetector

    private lateinit var btnStart : MaterialButton
    private lateinit var btnSetGoals : MaterialButton

    private var stepSensorManager: StepSensorManager? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startStepCounter()
        } else {
            Toast.makeText(requireContext(), "Permission denied for step counting", Toast.LENGTH_SHORT).show()
        }
    }


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
        setupWaterClickEvents()


        homeViewModel.loadTodayWaterLog()
        homeViewModel.loadUserGoals()
        homeViewModel.loadTodayCalories()
        homeViewModel.loadTodaySteps()

        checkStepPermission()
    }

    private fun checkStepPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startStepCounter()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            startStepCounter()
        }
    }

    private fun startStepCounter() {
        stepSensorManager = StepSensorManager(requireContext()) { steps ->
            homeViewModel.updateSteps(steps)
        }
        stepSensorManager?.startListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stepSensorManager?.stopListening()
    }


    private fun setupWaterClickEvents() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            // 1. Xử lý chạm 1 lần (Single Tap)
            override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                homeViewModel.addWater(250)
                return true
            }

            // 2. Xử lý chạm 2 lần (Double Tap)
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                homeViewModel.removeWater(250)
                return true
            }

            // 3. Bắt buộc: Phải trả về true ở onDown để GestureDetector bắt đầu hoạt động
            override fun onDown(e: android.view.MotionEvent): Boolean {
                return true
            }
        }

        val detector = GestureDetector(requireContext(), listener)

        // Gán vào biểu tượng giọt nước
        imgWaterIcon.setOnTouchListener { _, event ->
            // Chỉ cần gọi detector xử lý, không gọi performClick() ở đây nữa
            detector.onTouchEvent(event)
        }
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
        stepsValue.text = "0"
        stepsLabel.text = "Steps"

        // 2. Calories
        caloriesIcon.setImageResource(R.drawable.ic_calories)
        caloriesValue.text = "0"
        caloriesLabel.text = "Active Kcal"

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
        dialog.requestWindowFeature(Window. FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_set_goals)
        dialog.window?. setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams. MATCH_PARENT,
            ViewGroup.LayoutParams. WRAP_CONTENT
        )

        // Initialize dialog views
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val btnSaveGoals = dialog.findViewById<MaterialButton>(R.id.btnSaveGoals)

        // Steps controls
        val tvStepsValue = dialog.findViewById<TextView>(R.id.tvStepsValue)
        val tvStepsTarget = dialog.findViewById<TextView>(R. id.tvStepsTarget)
        val sliderSteps = dialog.findViewById<Slider>(R. id.sliderSteps)
        val btnIncreaseSteps = dialog.findViewById<ImageView>(R.id. btnIncreaseSteps)
        val btnDecreaseSteps = dialog.findViewById<ImageView>(R.id.btnDecreaseSteps)

        // Water controls
        val tvWaterValue = dialog.findViewById<TextView>(R. id.tvWaterValue)
        val tvWaterTarget = dialog.findViewById<TextView>(R.id.tvWaterTarget)
        val sliderWater = dialog. findViewById<Slider>(R.id.sliderWater)
        val btnIncreaseWater = dialog.findViewById<ImageView>(R.id.btnIncreaseWater)
        val btnDecreaseWater = dialog.findViewById<ImageView>(R.id.btnDecreaseWater)

        // Active Calories controls
        val tvActiveCaloriesValue = dialog.findViewById<TextView>(R.id.tvActiveCaloriesValue)
        val tvActiveCaloriesTarget = dialog.findViewById<TextView>(R.id.tvActiveCaloriesTarget)
        val sliderActiveCalories = dialog.findViewById<Slider>(R.id.sliderActiveCalories)
        val btnIncreaseActiveCalories = dialog.findViewById<ImageView>(R.id.btnIncreaseActiveCalories)
        val btnDecreaseActiveCalories = dialog. findViewById<ImageView>(R.id.btnDecreaseActiveCalories)

        // Calories Consume controls
        val tvCaloriesConsumeValue = dialog.findViewById<TextView>(R.id.tvCaloriesConsumeValue)
        val tvCaloriesConsumeTarget = dialog.findViewById<TextView>(R.id.tvCaloriesConsumeTarget)
        val sliderCaloriesConsume = dialog.findViewById<Slider>(R.id.sliderCaloriesConsume)
        val btnIncreaseCaloriesConsume = dialog.findViewById<ImageView>(R.id.btnIncreaseCaloriesConsume)
        val btnDecreaseCaloriesConsume = dialog.findViewById<ImageView>(R.id.btnDecreaseCaloriesConsume)

        // Weekly Running controls
        val tvWeeklyRunningValue = dialog.findViewById<TextView>(R.id.tvWeeklyRunningValue)
        val tvWeeklyRunningTarget = dialog.findViewById<TextView>(R.id.tvWeeklyRunningTarget)
        val sliderWeeklyRunning = dialog.findViewById<Slider>(R.id.sliderWeeklyRunning)
        val btnIncreaseWeeklyRunning = dialog.findViewById<ImageView>(R.id.btnIncreaseWeeklyRunning)
        val btnDecreaseWeeklyRunning = dialog. findViewById<ImageView>(R.id.btnDecreaseWeeklyRunning)

        // ✨ Load current goals from ViewModel
        val user = homeViewModel.user.value

        // ✨ FIXED: Validate values to match slider ranges
        var stepsGoal = (user?.dailyStepsGoal ?: 0).let {
            if (it == 0) 10000 else it. coerceIn(1000, 30000)
        }

        var waterGoal = (user?.dailyWaterGoal ?: 0).let {
            if (it == 0) 2000 else it.coerceIn(500, 5000)
        }

        var activeCaloriesGoal = (user?.dailyActiveCalories ?: 0).let {
            if (it == 0) 500 else it.coerceIn(100, 2000)
        }

        var caloriesConsumeGoal = (user?.dailyCaloriesConsume ?: 0).let {
            if (it == 0) 2000 else it.coerceIn(1000, 5000)
        }

        var weeklyRunningGoal = (user?.weeklyRunning ?: 0).let {
            if (it == 0) 20 else it.coerceIn(5, 100)
        }

        // ✨ Setup initial values - Steps
        sliderSteps.value = stepsGoal. toFloat()
        tvStepsValue.text = String.format("%,d", stepsGoal)
        tvStepsTarget.text = if (user?.dailyStepsGoal == 0) {
            "Target:  None (tap to set)"
        } else {
            "Target: ${String. format("%,d", stepsGoal)} steps"
        }

        // Water
        sliderWater.value = waterGoal.toFloat()
        tvWaterValue.text = String.format("%,d", waterGoal)
        tvWaterTarget.text = if (user?.dailyWaterGoal == 0) {
            "Target: None (tap to set)"
        } else {
            "Target: ${String.format("%,d", waterGoal)} ml"
        }

        // Active Calories
        sliderActiveCalories. value = activeCaloriesGoal.toFloat()
        tvActiveCaloriesValue.text = activeCaloriesGoal.toString()
        tvActiveCaloriesTarget.text = if (user?.dailyActiveCalories == 0) {
            "Target: None (tap to set)"
        } else {
            "Target: $activeCaloriesGoal kcal"
        }

        // Calories Consume
        sliderCaloriesConsume.value = caloriesConsumeGoal. toFloat()
        tvCaloriesConsumeValue.text = String.format("%,d", caloriesConsumeGoal)
        tvCaloriesConsumeTarget.text = if (user?. dailyCaloriesConsume == 0) {
            "Target: None (tap to set)"
        } else {
            "Target: ${String.format("%,d", caloriesConsumeGoal)} kcal"
        }

        // Weekly Running
        sliderWeeklyRunning.value = weeklyRunningGoal.toFloat()
        tvWeeklyRunningValue.text = weeklyRunningGoal.toString()
        tvWeeklyRunningTarget.text = if (user?.weeklyRunning == 0) {
            "Target: None (tap to set)"
        } else {
            "Target: $weeklyRunningGoal km"
        }

        // Setup Steps Slider
        sliderSteps.addOnChangeListener { _, value, _ ->
            stepsGoal = value.toInt()
            tvStepsValue. text = String.format("%,d", stepsGoal)
            tvStepsTarget.text = "Target: ${String.format("%,d", stepsGoal)} steps"
        }

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
            tvWaterValue. text = String.format("%,d", waterGoal)
            tvWaterTarget.text = "Target: ${String.format("%,d", waterGoal)} ml"
        }

        btnIncreaseWater.setOnClickListener {
            val newValue = (sliderWater.value + 250).coerceAtMost(sliderWater.valueTo)
            sliderWater.value = newValue
        }

        btnDecreaseWater.setOnClickListener {
            val newValue = (sliderWater. value - 250).coerceAtLeast(sliderWater.valueFrom)
            sliderWater.value = newValue
        }

        // Setup Active Calories Slider
        sliderActiveCalories.addOnChangeListener { _, value, _ ->
            activeCaloriesGoal = value. toInt()
            tvActiveCaloriesValue.text = activeCaloriesGoal.toString()
            tvActiveCaloriesTarget. text = "Target: $activeCaloriesGoal kcal"
        }

        btnIncreaseActiveCalories.setOnClickListener {
            val newValue = (sliderActiveCalories.value + 50).coerceAtMost(sliderActiveCalories.valueTo)
            sliderActiveCalories.value = newValue
        }

        btnDecreaseActiveCalories.setOnClickListener {
            val newValue = (sliderActiveCalories.value - 50).coerceAtLeast(sliderActiveCalories.valueFrom)
            sliderActiveCalories.value = newValue
        }

        // Setup Calories Consume Slider
        sliderCaloriesConsume.addOnChangeListener { _, value, _ ->
            caloriesConsumeGoal = value.toInt()
            tvCaloriesConsumeValue.text = String.format("%,d", caloriesConsumeGoal)
            tvCaloriesConsumeTarget.text = "Target: ${String.format("%,d", caloriesConsumeGoal)} kcal"
        }

        btnIncreaseCaloriesConsume.setOnClickListener {
            val newValue = (sliderCaloriesConsume.value + 100).coerceAtMost(sliderCaloriesConsume. valueTo)
            sliderCaloriesConsume.value = newValue
        }

        btnDecreaseCaloriesConsume. setOnClickListener {
            val newValue = (sliderCaloriesConsume.value - 100).coerceAtLeast(sliderCaloriesConsume. valueFrom)
            sliderCaloriesConsume.value = newValue
        }

        // Setup Weekly Running Slider
        sliderWeeklyRunning.addOnChangeListener { _, value, _ ->
            weeklyRunningGoal = value.toInt()
            tvWeeklyRunningValue. text = weeklyRunningGoal.toString()
            tvWeeklyRunningTarget.text = "Target: $weeklyRunningGoal km"
        }

        btnIncreaseWeeklyRunning.setOnClickListener {
            val newValue = (sliderWeeklyRunning.value + 5).coerceAtMost(sliderWeeklyRunning.valueTo)
            sliderWeeklyRunning.value = newValue
        }

        btnDecreaseWeeklyRunning.setOnClickListener {
            val newValue = (sliderWeeklyRunning.value - 5).coerceAtLeast(sliderWeeklyRunning.valueFrom)
            sliderWeeklyRunning.value = newValue
        }

        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Save button
        btnSaveGoals.setOnClickListener {
            homeViewModel.saveGoals(
                waterGoal = waterGoal,
                stepsGoal = stepsGoal,
                activeCalories = activeCaloriesGoal,
                caloriesConsume = caloriesConsumeGoal,
                weeklyRunning = weeklyRunningGoal
            )
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

        // Cập nhật Calories
        homeViewModel.totalCalories.observe(viewLifecycleOwner) { calories ->
            caloriesValue.text = String.format("%,d", calories)
        }

        // Cập nhật Steps
        homeViewModel.todaySteps.observe(viewLifecycleOwner) { steps ->
            stepsValue.text = String.format("%,d", steps)
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
