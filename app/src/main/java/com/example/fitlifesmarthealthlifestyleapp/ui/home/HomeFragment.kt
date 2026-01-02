package com.example.fitlifesmarthealthlifestyleapp.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WaterLog
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

