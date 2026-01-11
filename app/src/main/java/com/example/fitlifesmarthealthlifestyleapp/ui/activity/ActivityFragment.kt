package com.example.fitlifesmarthealthlifestyleapp.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fitlifesmarthealthlifestyleapp.R
import kotlin.math.roundToInt


class ActivityFragment : Fragment() {
    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvPace: TextView
    private lateinit var tvAvgSpeed: TextView
    private lateinit var tvCalories: TextView

    private lateinit var btnStartActivity: Button
    private lateinit var btnPause: Button
    private lateinit var btnComplete: Button
    private lateinit var layoutButtons: LinearLayout

    private var isRunning = false
    private var isPaused = false
    private var seconds = 0
    private var distance = 0.0 // in km
    private var calories = 0

    private val handler = Handler(Looper.getMainLooper())

    // Runnable để update UI mỗi giây
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                seconds++
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
    }

    // Runnable để simulate activity
    private val activitySimulator = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                // Simulate distance increase (thay bằng GPS thực tế)
                distance += 0.002 // ~0.002 km/s = ~7.2 km/h
                calories = (distance * 60).roundToInt()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        updateUI()
    }

    private fun initViews(view: View) {
        // Khởi tạo các TextView
        tvTime = view.findViewById(R.id.tvTime)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvPace = view.findViewById(R.id.tvPace)
        tvAvgSpeed = view.findViewById(R.id.tvAvgSpeed)
        tvCalories = view.findViewById(R.id.tvCalories)

        // Khởi tạo các Button
        btnStartActivity = view.findViewById(R.id.btnStartActivity)
        btnPause = view.findViewById(R.id.btnPause)
        btnComplete = view.findViewById(R.id.btnComplete)
        layoutButtons = view.findViewById(R.id.layoutButtons)
    }

    private fun setupListeners() {
        // Click Start Activity
        btnStartActivity.setOnClickListener {
            startActivity()
        }

        // Click Pause/Resume
        btnPause.setOnClickListener {
            if (isPaused) {
                resumeActivity()
            } else {
                pauseActivity()
            }
        }

        // Click Complete
        btnComplete.setOnClickListener {
            completeActivity()
        }
    }

    private fun startActivity() {
        isRunning = true
        isPaused = false

        // Ẩn nút Start, hiện nút Pause và Complete
        btnStartActivity.visibility = View.GONE
        layoutButtons.visibility = View.VISIBLE

        // Bắt đầu đếm thời gian
        handler.post(updateRunnable)

        // Bắt đầu simulate activity
        handler.post(activitySimulator)
    }

    private fun pauseActivity() {
        isPaused = true
        btnPause.text = "Resume Activity"

        // Dừng các handler
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(activitySimulator)
    }

    private fun resumeActivity() {
        isPaused = false
        btnPause.text = "Pause Activity"

        // Tiếp tục chạy
        handler.post(updateRunnable)
        handler.post(activitySimulator)
    }

    private fun completeActivity() {
        isRunning = false
        isPaused = false

        // Lưu dữ liệu activity (nếu cần)
        saveActivityData()

        // Reset tất cả về 0
        resetActivity()

        // Hiện nút Start, ẩn nút Pause và Complete
        btnStartActivity.visibility = View.VISIBLE
        layoutButtons.visibility = View.GONE
        btnPause.text = "Pause Activity"

        // Dừng tất cả handler
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(activitySimulator)

        // Update UI
        updateUI()
    }

    private fun resetActivity() {
        seconds = 0
        distance = 0.0
        calories = 0
    }

    private fun updateUI() {
        // Update Time (MM:SS)
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        tvTime.text = String.format("%02d:%02d", minutes, secs)

        // Update Distance
        tvDistance.text = String.format("%.2f km", distance)

        // Calculate và update Pace (min/km)
        val pace = if (distance > 0) {
            val paceInMinutes = (seconds / 60.0) / distance
            val paceMin = paceInMinutes.toInt()
            val paceSec = ((paceInMinutes - paceMin) * 60).toInt()
            String.format("%d:%02d", paceMin, paceSec)
        } else {
            "0:00"
        }
        tvPace.text = pace

        // Calculate và update Average Speed (km/h)
        val avgSpeed = if (seconds > 0) {
            (distance / (seconds / 3600.0))
        } else {
            0.0
        }
        tvAvgSpeed.text = String.format("%.1f km/h", avgSpeed)

        // Update Calories
        tvCalories.text = "$calories kcal"
    }

    private fun saveActivityData() {
        // TODO: Lưu vào database hoặc SharedPreferences
        // Có thể dùng Room Database hoặc DataStore

        println("=== Activity Completed ===")
        println("Time: ${tvTime.text}")
        println("Distance: ${tvDistance.text}")
        println("Pace: ${tvPace.text}")
        println("Avg Speed: ${tvAvgSpeed.text}")
        println("Calories: ${tvCalories.text}")
        println("========================")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup handler khi fragment bị destroy
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(activitySimulator)
    }

    companion object {
        fun newInstance() = ActivityFragment()
    }
}