package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WorkoutProgram

class WorkoutAdapter(
    private var list: List<WorkoutProgram>,
    private val onClick: (WorkoutProgram) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgThumb)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val time: TextView = view.findViewById(R.id.tvTime)
        val cal: TextView = view.findViewById(R.id.tvCal)
        val badge: TextView = view.findViewById(R.id.tvDifficulty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item.name
        holder.time.text = "🕒 ${item.durationMins} mins"
        holder.cal.text = "🔥 ${item.caloriesBurn} cal"
        holder.badge.text = item.difficulty

        // Đổi màu badge
        if (item.difficulty == "Beginner") {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_beginner)
        } else if(item.difficulty == "Advanced"){
            holder.badge.setBackgroundResource(R.drawable.bg_badge_advanced)
        }
        else {
            holder.badge.setBackgroundResource(R.drawable.bg_badge_intermediate)
        }

        Glide.with(holder.itemView).load(item.imageUrl).centerCrop().into(holder.img)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<WorkoutProgram>) {
        list = newList
        notifyDataSetChanged()
    }
}