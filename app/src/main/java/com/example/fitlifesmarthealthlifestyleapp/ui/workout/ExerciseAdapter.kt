package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Exercise

class ExerciseAdapter(private var list: List<Exercise>) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgGif: ImageView = view.findViewById(R.id.ivExerciseGif)
        val tvName: TextView = view.findViewById(R.id.tvExerciseName)
        val tvTime: TextView = view.findViewById(R.id.tvExerciseTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = "${item.order}. ${item.name}"
        holder.tvTime.text = "${item.durationSeconds}s work - ${item.restSeconds}s rest"

        // Load ảnh GIF bằng Glide
        Glide.with(holder.itemView)
            .asGif() // Bắt buộc Glide tải dưới dạng ảnh động
            .load(item.gifUrl)
            .centerCrop()
            .into(holder.imgGif)
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Exercise>) {
        list = newList
        notifyDataSetChanged()
    }
}