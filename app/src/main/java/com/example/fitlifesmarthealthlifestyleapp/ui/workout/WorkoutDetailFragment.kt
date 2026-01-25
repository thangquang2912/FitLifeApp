package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.button.MaterialButton

class WorkoutDetailFragment : Fragment() {

    // Lấy argument do SafeArgs tự tạo ra
    private val args: WorkoutDetailFragmentArgs by navArgs()

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

        // Lấy dữ liệu từ argument
        val program = args.workoutProgram

        // Cập nhật UI
        tvTitle.text = program.name
        tvCategory.text = program.category
        tvDifficulty.text = program.difficulty
        tvTime.text = "🕒 ${program.durationMins} mins"
        tvCal.text = "🔥 ${program.caloriesBurn} cal"
        tvDesc.text = program.description

        // Load ảnh bằng Glide
        Glide.with(this)
            .load(program.imageUrl)
            .centerCrop()
            .into(ivThumb)

        // Xử lý nút Back
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Xử lý nút Bắt đầu tập
        btnStart.setOnClickListener {
            // TODO: Điều hướng sang màn hình video bài tập hoặc bấm giờ
        }
    }
}