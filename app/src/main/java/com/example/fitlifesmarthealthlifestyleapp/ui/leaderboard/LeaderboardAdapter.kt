package com.example.fitlifesmarthealthlifestyleapp.ui.leaderboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LeaderboardUser
import java.text.DecimalFormat

class LeaderboardAdapter(
    private var users: List<LeaderboardUser>,
    private var isSteps: Boolean = false
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val ivTrophy: ImageView = view.findViewById(R.id.ivTrophy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val rank = position + 1

        holder.tvRank.text = rank.toString()
        holder.tvName.text = user.name

        // Format Score
        val formatter = DecimalFormat("#,###")
        if (isSteps) {
            holder.tvScore.text = "${formatter.format(user.totalSteps)} steps"
        } else {
            holder.tvScore.text = String.format("%.1f km", user.totalDistanceKm)
        }

        // Load Avatar
        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .fallback(R.drawable.ic_user)
            .circleCrop()
            .into(holder.ivAvatar)

        // --- XỬ LÝ MÀU SẮC CHO TOP 3 ---
        // Reset về mặc định trước (cho các hạng >= 4)
        holder.tvRank.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F0F0")) // Xám nhạt
        holder.tvRank.setTextColor(Color.parseColor("#555555")) // Chữ xám đậm
        holder.ivTrophy.visibility = View.INVISIBLE // Ẩn cúp

        when (rank) {
            1 -> {
                // Hạng 1: Nền Vàng, Chữ Trắng, Hiện Cúp Vàng
                holder.tvRank.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
                holder.tvRank.setTextColor(Color.WHITE)

                holder.ivTrophy.visibility = View.VISIBLE
                holder.ivTrophy.setColorFilter(Color.parseColor("#FFD700"))
                holder.ivTrophy.setImageResource(R.drawable.ic_king)
            }
            2 -> {
                // Hạng 2: Nền Bạc
                holder.tvRank.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C0C0C0"))
                holder.tvRank.setTextColor(Color.WHITE)

                holder.ivTrophy.visibility = View.VISIBLE
                holder.ivTrophy.setColorFilter(Color.parseColor("#C0C0C0"))
            }
            3 -> {
                // Hạng 3: Nền Đồng
                holder.tvRank.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CD7F32"))
                holder.tvRank.setTextColor(Color.WHITE)

                holder.ivTrophy.visibility = View.VISIBLE
                holder.ivTrophy.setColorFilter(Color.parseColor("#CD7F32"))
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<LeaderboardUser>, isStepsMode: Boolean) {
        this.users = newUsers
        this.isSteps = isStepsMode
        notifyDataSetChanged()
    }
}