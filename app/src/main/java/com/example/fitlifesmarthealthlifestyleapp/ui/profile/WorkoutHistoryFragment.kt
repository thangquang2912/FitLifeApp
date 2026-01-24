package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R

class WorkoutHistoryFragment : Fragment() {

    private lateinit var viewModel: WorkoutHistoryViewModel
    private lateinit var adapter: WorkoutHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_workout_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // System back -> Profile
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack(R.id.profile, false)
                }
            }
        )

        // NEW: Back button in UI -> Profile
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().popBackStack(R.id.profile, false)
        }

        viewModel = ViewModelProvider(this)[WorkoutHistoryViewModel::class.java]

        adapter = WorkoutHistoryAdapter { log ->
            val bundle = Bundle().apply { putString("activityId", log.id) }
            findNavController().navigate(R.id.action_workoutHistory_to_activityDetails, bundle)
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.toast.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.loadHistory()
    }
}