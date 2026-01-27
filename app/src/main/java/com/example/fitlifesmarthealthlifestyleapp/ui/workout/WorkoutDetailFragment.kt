package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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

class WorkoutDetailFragment : Fragment() {

    // Lấy argument do SafeArgs tự tạo ra
    private val args: WorkoutDetailFragmentArgs by navArgs()

    // Khởi tạo Repository và Adapter
    private val repository = WorkoutRepository()
    private lateinit var exerciseAdapter: ExerciseAdapter

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

        // THÊM: Ánh xạ RecyclerView cho danh sách động tác
        val rvExercises = view.findViewById<RecyclerView>(R.id.rvExercises)

        // Lấy dữ liệu từ argument
        val program = args.workoutProgram

        // Cập nhật UI thông tin cơ bản
        tvTitle.text = program.name
        tvCategory.text = program.category
        tvDifficulty.text = program.difficulty
        if(program.difficulty == "Beginner") {tvDifficulty.setBackgroundResource(R.drawable.bg_badge_beginner)}
        else if(program.difficulty == "Intermediate") {tvDifficulty.setBackgroundResource(R.drawable.bg_badge_intermediate)}
        else if(program.difficulty == "Advanced") {tvDifficulty.setBackgroundResource(R.drawable.bg_badge_advanced)}

        tvTime.text = "${program.durationMins} mins"
        tvCal.text = "${program.caloriesBurn} cal"
        tvDesc.text = program.description

        // Load ảnh bằng Glide
        Glide.with(this)
            .load(program.imageUrl)
            .centerCrop()
            .into(ivThumb)

        // -------------------------------------------------------------------
        // XỬ LÝ DANH SÁCH ĐỘNG TÁC (EXERCISES)
        // -------------------------------------------------------------------
        // 1. Cài đặt Adapter
        exerciseAdapter = ExerciseAdapter(emptyList())
        rvExercises.layoutManager = LinearLayoutManager(requireContext())
        rvExercises.adapter = exerciseAdapter

        // 2. Lấy dữ liệu từ Firebase dựa vào ID của bài tập hiện tại
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.getExercisesByProgramId(program.id)
            if (result.isSuccess) {
                val exercises = result.getOrDefault(emptyList())
                exerciseAdapter.updateData(exercises)
            }
        }
        // -------------------------------------------------------------------

        // Xử lý nút Back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý nút Start
        btnStart.setOnClickListener {
            val action = WorkoutDetailFragmentDirections.actionDetailToPlayer(program)
            findNavController().navigate(action)
        }
    }
}