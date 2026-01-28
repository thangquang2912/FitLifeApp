package com.example.fitlifesmarthealthlifestyleapp.ui.social

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

        // 1. Hiển thị thông tin
        holder.tvContent.text = "${item.senderName} ${item.message}"
        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        holder.tvTime.text = sdf.format(item.timestamp.toDate())
        Glide.with(context).load(item.senderAvatar).placeholder(R.drawable.ic_user).circleCrop().into(holder.ivAvatar)

        // Icon loại thông báo
        val iconRes = if (item.type == "MESSAGE") R.drawable.ic_chat else R.drawable.ic_notifications
        holder.ivType.setImageResource(iconRes)

        // 2. LOGIC MÀU NỀN (Quan trọng)
        if (!item.isRead) {
            // Chưa đọc (false) -> Màu Cam
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.orange_light_bg))
            holder.tvContent.setTypeface(null, Typeface.BOLD) // Chữ đậm
            holder.ivUnreadDot.visibility = View.VISIBLE
        } else {
            // Đã đọc (true) -> Màu Trắng
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            holder.tvContent.setTypeface(null, Typeface.NORMAL)
            holder.ivUnreadDot.visibility = View.GONE
        }

        // 3. SỰ KIỆN CLICK
        holder.itemView.setOnClickListener {
            // Nếu chưa đọc -> Đổi sang đã đọc NGAY LẬP TỨC tại client
            if (!item.isRead) {
                item.isRead = true // Update Model

                // Update UI ngay lập tức (không đợi Firebase)
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                holder.tvContent.setTypeface(null, Typeface.NORMAL)
                holder.ivUnreadDot.visibility = View.GONE
            }
            // Gọi callback để Fragment xử lý tiếp (Update Firebase & Chuyển trang)
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