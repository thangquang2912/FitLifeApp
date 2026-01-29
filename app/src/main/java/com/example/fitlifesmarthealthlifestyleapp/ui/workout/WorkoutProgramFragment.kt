package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import androidx.navigation.fragment.findNavController
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WorkoutProgram
import com.google.android.material.chip.ChipGroup

class WorkoutProgramFragment : Fragment() {

    private val viewModel: WorkoutViewModel by viewModels()
    private lateinit var rvWorkouts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: WorkoutAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_workout_program, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvWorkouts = view.findViewById(R.id.rvWorkouts)
        progressBar = view.findViewById(R.id.progressBar)
        chipGroup = view.findViewById(R.id.chipGroupFilter)

        setupAdapter()
        setupListeners()

        viewModel.loadPrograms()

        viewModel.isLoading.observe(viewLifecycleOwner) {
            progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

            // Lắng nghe danh sách hiển thị
        viewModel.displayPrograms.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun setupAdapter() {
        adapter = WorkoutAdapter(emptyList()) { program ->
            val action = WorkoutProgramFragmentDirections.actionWorkoutListToWorkoutDetail(program)
            findNavController().navigate(action)
        }
        rvWorkouts.layoutManager = LinearLayoutManager(context)
        rvWorkouts.adapter = adapter
    }

    private fun setupListeners() {
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> viewModel.filterPrograms(WorkoutCategory.ALL)
                R.id.chipWeightLoss -> viewModel.filterPrograms(WorkoutCategory.WEIGHT_LOSS)
                R.id.chipMuscleGain -> viewModel.filterPrograms(WorkoutCategory.MUSCLE_GAIN)
                R.id.chipYoga -> viewModel.filterPrograms(WorkoutCategory.YOGA)
                else -> viewModel.filterPrograms(WorkoutCategory.ALL)
            }
        }
    }
}