package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Meal

class MealAdapter : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_log, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgMeal: ImageView = itemView.findViewById(R.id.imgMeal)
        private val tvMealName: TextView = itemView.findViewById(R.id.tvMealName)
        private val tvMealCalories: TextView = itemView.findViewById(R.id.tvMealCalories)
        private val tvCarb: TextView = itemView.findViewById(R.id.tvCarbDetail)
        private val tvPro: TextView = itemView.findViewById(R.id.tvProDetail)
        private val tvFat: TextView = itemView.findViewById(R.id.tvFatDetail)

        fun bind(meal: Meal) {
            tvMealName.text = meal.name
            tvMealCalories.text = "${meal.calories} kcal"

            // Format số lẻ thành int cho gọn
            tvCarb.text = "Carbs: ${meal.carbs.toInt()}g"
            tvPro.text = "Protein: ${meal.protein.toInt()}g"
            tvFat.text = "Fat: ${meal.fat.toInt()}g"

            // Load ảnh (dùng thư viện Glide)
            if (!meal.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(meal.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background) // Thay bằng ảnh placeholder của bạn
                    .into(imgMeal)
            } else {
                imgMeal.setImageResource(R.drawable.ic_launcher_background) // Ảnh mặc định
            }
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem == newItem
    }
}