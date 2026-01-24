package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import java.text.SimpleDateFormat
import java.util.Locale

class WorkoutHistoryAdapter(
    private val onClick: (ActivityLog) -> Unit
) : ListAdapter<ActivityLog, WorkoutHistoryAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_workout_history, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(itemView: View, private val onClick: (ActivityLog) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val df = SimpleDateFormat("MMM d, yyyy", Locale.US)

        fun bind(item: ActivityLog) {
            tvName.text = item.activityType
            tvDate.text = df.format(item.startTime.toDate())
            tvDistance.text = String.format("%.2f km", item.distanceKm)
            tvDuration.text = formatDuration(item.durationSeconds)

            itemView.setOnClickListener { onClick(item) }
        }

        private fun formatDuration(sec: Int): String {
            val m = (sec % 3600) / 60
            val s = sec % 60
            return String.format("%02d:%02d", m, s)
        }
    }
}