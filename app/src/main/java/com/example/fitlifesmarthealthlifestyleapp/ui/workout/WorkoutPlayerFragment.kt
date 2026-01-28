package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WorkoutRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Exercise
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class WorkoutPlayerFragment : Fragment() {

    private val args: WorkoutPlayerFragmentArgs by navArgs()
    private val repository = WorkoutRepository()

    // Dữ liệu
    private var exerciseList = emptyList<Exercise>()
    private var currentExerciseIndex = 0
    private var isResting = false // Trạng thái: Đang nghỉ hay Đang tập

    // Timer
    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining = 0L // Thời gian còn lại (giây)
    private var isPaused = false

    // Views
    private lateinit var tvExerciseName: TextView
    private lateinit var ivGif: ImageView
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var btnPause: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var cardGif: MaterialCardView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_workout_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ánh xạ View
        tvExerciseName = view.findViewById(R.id.tvExerciseName)
        ivGif = view.findViewById(R.id.ivExerciseGif)
        tvTimer = view.findViewById(R.id.tvTimer)
        tvProgress = view.findViewById(R.id.tvProgress)
        btnPause = view.findViewById(R.id.btnPauseResume)
        tvStatus = view.findViewById(R.id.tvStatus)
        cardGif = view.findViewById(R.id.cardGif)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnSkip = view.findViewById<ImageButton>(R.id.btnSkip)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevious)

        // 2. Lấy dữ liệu & Tải danh sách
        val program = args.workoutProgram
        loadExercises(program.id)

        // 3. Xử lý sự kiện Click
        btnClose.setOnClickListener {
            // Xác nhận trước khi thoát nếu đang tập dở
            AlertDialog.Builder(requireContext())
                .setTitle("Quit Workout?")
                .setMessage("Are you sure you want to quit?")
                .setPositiveButton("Quit") { _, _ -> findNavController().navigateUp() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnPause.setOnClickListener {
            togglePause()
        }

        btnSkip.setOnClickListener {
            countDownTimer?.cancel()
            nextExercise()
        }

        btnPrev.setOnClickListener {
            countDownTimer?.cancel()
            if (isResting) {
                startWorkoutPhase()
            } else {
                if (currentExerciseIndex > 0) {
                    currentExerciseIndex--
                    startWorkoutPhase()
                } else {
                    startWorkoutPhase()
                    Toast.makeText(context, "Restarting exercise!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadExercises(programId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getExercisesByProgramId(programId)
            if (result.isSuccess) {
                val rawList = result.getOrDefault(emptyList())

                val expandedList = mutableListOf<Exercise>()
                for (exercise in rawList) {
                    val totalSets = if (exercise.sets <= 0) 1 else exercise.sets
                    for (i in 1..totalSets) {
                        val newName = if (totalSets > 1) "${exercise.name} (Set $i/$totalSets)" else exercise.name
                        expandedList.add(exercise.copy(name = newName))
                    }
                }
                exerciseList = expandedList

                if (exerciseList.isNotEmpty()) {
                    startWorkoutPhase()
                } else {
                    Toast.makeText(context, "No exercises found!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startWorkoutPhase() {
        isResting = false
        val exercise = exerciseList[currentExerciseIndex]

        tvStatus.text = "WORK"
        tvStatus.background = resources.getDrawable(R.drawable.bg_badge_beginner, null)
        tvExerciseName.text = exercise.name
        tvProgress.text = "${currentExerciseIndex + 1} / ${exerciseList.size}"

        cardGif.visibility = View.VISIBLE
        Glide.with(this).asGif().load(exercise.gifUrl).into(ivGif)

        startTimer(exercise.durationSeconds.toLong())
    }

    private fun startRestPhase() {
        val exercise = exerciseList[currentExerciseIndex]
        if (exercise.restSeconds <= 0) {
            nextExercise()
            return
        }

        isResting = true
        tvStatus.text = "REST"
        tvStatus.background = resources.getDrawable(R.drawable.bg_badge_intermediate, null)
        tvExerciseName.text = "Next: ${exerciseList.getOrNull(currentExerciseIndex + 1)?.name ?: "Finish"}"
        cardGif.visibility = View.INVISIBLE

        startTimer(exercise.restSeconds.toLong())
    }

    private fun startTimer(durationSeconds: Long) {
        countDownTimer?.cancel()
        timeRemaining = durationSeconds
        isPaused = false
        btnPause.text = "Pause"
        btnPause.setIconResource(android.R.drawable.ic_media_pause)

        countDownTimer = object : CountDownTimer(timeRemaining * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished / 1000
                updateTimerUI()
            }

            override fun onFinish() {
                timeRemaining = 0
                updateTimerUI()
                nextStep()
            }
        }.start()
    }

    private fun updateTimerUI() {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        tvTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun nextStep() {
        if (isResting) {
            nextExercise()
        } else {
            if (currentExerciseIndex == exerciseList.size - 1) {
                finishWorkout()
            } else {
                startRestPhase()
            }
        }
    }

    private fun nextExercise() {
        currentExerciseIndex++
        if (currentExerciseIndex < exerciseList.size) {
            startWorkoutPhase()
        } else {
            finishWorkout()
        }
    }

    private fun finishWorkout() {
        // Hủy timer để tránh chạy ngầm
        countDownTimer?.cancel()

        // Hiển thị hộp thoại Chúc mừng + Hỏi ghi lịch
        AlertDialog.Builder(requireContext())
            .setTitle("Workout Completed!")
            .setMessage("Congratulations! Do you want to log this achievement to your Calendar?")
            .setPositiveButton("Log to Calendar") { _, _ ->
                // Gọi hàm ghi lịch
                logCompletionToCalendar()
                // Thoát màn hình
                findNavController().navigateUp()
            }
            .setNegativeButton("Skip") { _, _ ->
                // Chỉ thoát màn hình
                findNavController().navigateUp()
            }
            .setCancelable(false) // Bắt buộc chọn
            .show()
    }

    // Hàm tạo sự kiện "Đã xong" vào Calendar
    private fun logCompletionToCalendar() {
        val program = args.workoutProgram
        val durationMillis = (program.durationMins * 60 * 1000).toLong()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI

            // Dấu tích xanh để đánh dấu hoàn thành
            putExtra(CalendarContract.Events.TITLE, "✅ DONE: ${program.name}")

            putExtra(CalendarContract.Events.DESCRIPTION, "Finished workout ${program.name} on FitLife App!")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Home / Gym")

            // Thời gian: Ghi nhận NGAY LÚC NÀY
            val currentTime = System.currentTimeMillis()
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentTime + durationMillis)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Calendar app not found!", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------

    private fun togglePause() {
        if (isPaused) {
            startTimer(timeRemaining)
        } else {
            countDownTimer?.cancel()
            isPaused = true
            btnPause.text = "Resume"
            btnPause.setIconResource(android.R.drawable.ic_media_play)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}