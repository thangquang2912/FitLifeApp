package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Meal
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MealHistoryBottomSheet(
    private val mealList: List<Meal> // Nhận danh sách món ăn từ Fragment cha
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_meal_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvHistoryTitle)
        val rvMeals = view.findViewById<RecyclerView>(R.id.rvMealHistory)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyHistory)

        tvTitle.text = "Meal History (${mealList.size})"

        if (mealList.isNotEmpty()) {
            tvEmpty.visibility = View.GONE
            rvMeals.visibility = View.VISIBLE

            val adapter = MealAdapter()
            rvMeals.layoutManager = LinearLayoutManager(context)
            rvMeals.adapter = adapter
            adapter.submitList(mealList)
        } else {
            tvEmpty.visibility = View.VISIBLE
            rvMeals.visibility = View.GONE
        }
    }
}