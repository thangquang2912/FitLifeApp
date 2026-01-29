package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    fun updateList(newList: List<Notification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        val context = holder.itemView.context

        // 1. HIỂN THỊ THÔNG TIN NỘI DUNG
        holder.tvContent.text = "${item.senderName} ${item.message}"

        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        holder.tvTime.text = sdf.format(item.timestamp.toDate())

        // 2. HIỂN THỊ AVATAR (Quan trọng: Load đúng ảnh người gửi)
        Glide.with(context)
            .load(item.senderAvatar)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .circleCrop() // Đảm bảo ảnh bo tròn theo layout
            .into(holder.ivAvatar)

        // Hiển thị Icon loại thông báo (Like/Comment/Message...)
        val iconTypeRes = when(item.type) {
            "MESSAGE" -> R.drawable.ic_chat
            else -> R.drawable.ic_notifications
        }
        holder.ivType.setImageResource(iconTypeRes)

        // 3. LOGIC MÀU NỀN (Chưa đọc: CAM, Đã đọc: TRẮNG)
        if (!item.isRead) {
            // TRẠNG THÁI CHƯA ĐỌC
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0")) // Màu cam nhạt
            holder.tvContent.setTypeface(null, Typeface.BOLD)
            holder.ivUnreadDot.visibility = View.VISIBLE
        } else {
            // TRẠNG THÁI ĐÃ ĐỌC
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            holder.tvContent.setTypeface(null, Typeface.NORMAL)
            holder.ivUnreadDot.visibility = View.GONE
        }

        // 4. SỰ KIỆN CLICK
        holder.itemView.setOnClickListener {
            if (!item.isRead) {
                // Cập nhật giao diện ngay lập tức sang màu trắng
                item.isRead = true
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                holder.tvContent.setTypeface(null, Typeface.NORMAL)
                holder.ivUnreadDot.visibility = View.GONE
            }
            // Gọi callback xử lý Firebase và điều hướng
            onItemClick(item)
        }
    }

    override fun getItemCount() = notifications.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivNotifAvatar)
        val ivType: ImageView = view.findViewById(R.id.ivNotifType)
        val tvContent: TextView = view.findViewById(R.id.tvNotifContent)
        val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
        val ivUnreadDot: ImageView = view.findViewById(R.id.ivUnreadDot)
    }
}