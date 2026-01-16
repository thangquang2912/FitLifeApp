package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.os.Bundle
import android.view.*
import android.widget.Toast
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

        viewModel = ViewModelProvider(this)[WorkoutHistoryViewModel::class.java]

        adapter = WorkoutHistoryAdapter { log ->
            val bundle = Bundle().apply { putString("activityId", log.id) }
            findNavController().navigate(R.id.activityDetailsFragment, bundle)
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