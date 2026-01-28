package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class WorkoutDetailFragment : Fragment() {

    private val args: WorkoutDetailFragmentArgs by navArgs()
    private val repository = WorkoutRepository()
    private lateinit var exerciseAdapter: ExerciseAdapter

    // [MỚI] Biến lưu thời gian người dùng chọn
    private val selectedCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_workout_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ánh xạ View
        val ivThumb = view.findViewById<ImageView>(R.id.ivDetailThumb)
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvCategory = view.findViewById<TextView>(R.id.tvDetailCategory)
        val tvDifficulty = view.findViewById<TextView>(R.id.tvDetailDifficulty)
        val tvTime = view.findViewById<TextView>(R.id.tvDetailTime)
        val tvCal = view.findViewById<TextView>(R.id.tvDetailCal)
        val tvDesc = view.findViewById<TextView>(R.id.tvDetailDescription)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val btnStart = view.findViewById<MaterialButton>(R.id.btnStartWorkout)
        val rvExercises = view.findViewById<RecyclerView>(R.id.rvExercises)

        // [MỚI] Ánh xạ nút Schedule
        val btnSchedule = view.findViewById<ImageButton>(R.id.btnSchedule)

        val program = args.workoutProgram

        // Setup UI (Giữ nguyên)
        tvTitle.text = program.name
        tvCategory.text = program.category
        tvDifficulty.text = program.difficulty

        when(program.difficulty) {
            "Beginner" -> tvDifficulty.setBackgroundResource(R.drawable.bg_badge_beginner)
            "Intermediate" -> tvDifficulty.setBackgroundResource(R.drawable.bg_badge_intermediate)
            "Advanced" -> tvDifficulty.setBackgroundResource(R.drawable.bg_badge_advanced)
        }

        tvTime.text = "${program.durationMins} mins"
        tvCal.text = "${program.caloriesBurn} cal"
        tvDesc.text = program.description

        Glide.with(this)
            .load(program.imageUrl)
            .centerCrop()
            .into(ivThumb)

        // Setup RecyclerView (Giữ nguyên)
        exerciseAdapter = ExerciseAdapter(emptyList())
        rvExercises.layoutManager = LinearLayoutManager(requireContext())
        rvExercises.adapter = exerciseAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getExercisesByProgramId(program.id)
            if (result.isSuccess) {
                val exercises = result.getOrDefault(emptyList())
                exerciseAdapter.updateData(exercises)
            }
        }

        // Sự kiện Click
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnStart.setOnClickListener {
            val action = WorkoutDetailFragmentDirections.actionDetailToPlayer(program)
            findNavController().navigate(action)
        }

        // [MỚI] Xử lý nút Lên lịch -> Mở DatePicker
        btnSchedule.setOnClickListener {
            showDateTimePicker()
        }
    }

    // --- CÁC HÀM XỬ LÝ LỊCH ---

    // 1. Chọn ngày
    private fun showDateTimePicker() {
        val currentYear = selectedCalendar.get(Calendar.YEAR)
        val currentMonth = selectedCalendar.get(Calendar.MONTH)
        val currentDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, month)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Chọn ngày xong -> Chọn giờ
            showTimePicker()
        }, currentYear, currentMonth, currentDay)

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    // 2. Chọn giờ
    private fun showTimePicker() {
        val currentHour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = selectedCalendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            selectedCalendar.set(Calendar.MINUTE, minute)

            // Có đủ thông tin -> Mở App Lịch
            openCalendarIntent()
        }, currentHour, currentMinute, true).show()
    }

    // 3. Gửi Intent sang Google Calendar
    private fun openCalendarIntent() {
        val program = args.workoutProgram

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI

            putExtra(CalendarContract.Events.TITLE, "Workout: ${program.name}")
            putExtra(CalendarContract.Events.DESCRIPTION, "Kế hoạch tập luyện bài ${program.name} trên ứng dụng FitLife.")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Home / Gym")

            // Thời gian bắt đầu
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, selectedCalendar.timeInMillis)

            // Thời gian kết thúc (Cộng thêm thời lượng bài tập)
            val durationMillis = (program.durationMins * 60 * 1000).toLong()
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, selectedCalendar.timeInMillis + durationMillis)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không tìm thấy ứng dụng Lịch!", Toast.LENGTH_SHORT).show()
        }
    }
}