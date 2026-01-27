package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.os.Bundle
import android.os.CountDownTimer
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
            findNavController().navigateUp()
        }

        btnPause.setOnClickListener {
            togglePause()
        }

        btnSkip.setOnClickListener {
            countDownTimer?.cancel()
            nextExercise()
        }


        btnPrev.setOnClickListener {
            // 1. Dừng đồng hồ hiện tại lại ngay
            countDownTimer?.cancel()

            if (isResting) {
                // Đang trong giờ nghỉ
                startWorkoutPhase()
            } else {
                // Đang trong giờ tập
                if (currentExerciseIndex > 0) {
                    currentExerciseIndex--
                    startWorkoutPhase()
                } else {
                    startWorkoutPhase()
                    Toast.makeText(context, "Bắt đầu lại bài tập!", Toast.LENGTH_SHORT).show()
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
                    // Nếu trên Firebase để sets = 0 hoặc null -> Coi như là 1 set
                    val totalSets = if (exercise.sets <= 0) 1 else exercise.sets

                    for (i in 1..totalSets) {
                        // Tạo bản sao của bài tập
                        val newName = if (totalSets > 1) {
                            "${exercise.name} (Set $i/$totalSets)"
                        } else {
                            exercise.name
                        }

                        // Copy data và sửa lại tên để hiển thị Set
                        expandedList.add(exercise.copy(name = newName))
                    }
                }

                exerciseList = expandedList

                if (exerciseList.isNotEmpty()) {
                    startWorkoutPhase() // BẮT ĐẦU
                } else {
                    Toast.makeText(context, "Bài tập này chưa có dữ liệu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 1. Bắt đầu giai đoạn TẬP (WORK)
    private fun startWorkoutPhase() {
        isResting = false
        val exercise = exerciseList[currentExerciseIndex]

        // Cập nhật giao diện
        tvStatus.text = "WORK"
        tvStatus.setTextColor(resources.getColor(R.color.black, null)) // Màu đen cho WORK
        tvExerciseName.text = exercise.name
        tvProgress.text = "${currentExerciseIndex + 1} / ${exerciseList.size}"

        // Hiện ảnh GIF
        cardGif.visibility = View.VISIBLE
        Glide.with(this).asGif().load(exercise.gifUrl).into(ivGif)

        // Bắt đầu đếm ngược
        startTimer(exercise.durationSeconds.toLong())
    }

    // 2. Bắt đầu giai đoạn NGHỈ (REST)
    private fun startRestPhase() {
        val exercise = exerciseList[currentExerciseIndex]

        // Nếu bài này không có thời gian nghỉ -> Chuyển luôn bài tiếp
        if (exercise.restSeconds <= 0) {
            nextExercise()
            return
        }

        isResting = true

        // Cập nhật giao diện nghỉ
        tvStatus.text = "REST"
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null)) // Màu xanh cho REST
        tvExerciseName.text = "Next: ${exerciseList.getOrNull(currentExerciseIndex + 1)?.name ?: "Finish"}"

        // Ẩn GIF đi cho đỡ rối mắt (hoặc hiện ảnh tĩnh)
        cardGif.visibility = View.INVISIBLE

        // Bắt đầu đếm ngược nghỉ
        startTimer(exercise.restSeconds.toLong())
    }

    // --- LOGIC TIMER ---

    private fun startTimer(durationSeconds: Long) {
        countDownTimer?.cancel() // Hủy timer cũ nếu có
        timeRemaining = durationSeconds
        isPaused = false
        btnPause.text = "Pause"
        btnPause.icon = resources.getDrawable(android.R.drawable.ic_media_pause, null)

        // Tạo Timer mới (nhảy mỗi 1 giây)
        countDownTimer = object : CountDownTimer(timeRemaining * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished / 1000
                updateTimerUI()
            }

            override fun onFinish() {
                timeRemaining = 0
                updateTimerUI()
                nextStep() // Hết giờ -> Chuyển bước
            }
        }.start()
    }

    private fun updateTimerUI() {
        // Định dạng 00:30
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        tvTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    // Chuyển bước: Work -> Rest -> Next Work
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
            startWorkoutPhase() // Còn bài -> Tập tiếp
        } else {
            finishWorkout() // Hết bài -> Kết thúc
        }
    }

    private fun finishWorkout() {
        Toast.makeText(context, "Chúc mừng! Bạn đã hoàn thành bài tập!", Toast.LENGTH_LONG).show()
        findNavController().navigateUp() // Quay về (Sau này sẽ chuyển sang màn hình Success)
    }

    // --- LOGIC PAUSE / RESUME ---

    private fun togglePause() {
        if (isPaused) {
            // Resume: Chạy lại timer với thời gian còn lại
            startTimer(timeRemaining)
        } else {
            // Pause: Hủy timer, giữ nguyên thời gian còn lại
            countDownTimer?.cancel()
            isPaused = true
            btnPause.text = "Resume"
            btnPause.icon = resources.getDrawable(android.R.drawable.ic_media_play, null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}