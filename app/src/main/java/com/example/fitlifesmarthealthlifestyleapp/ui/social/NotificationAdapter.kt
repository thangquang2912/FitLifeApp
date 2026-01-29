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

        // ===== 1. LOCALIZE MESSAGE (PATCH CHÍNH) =====
        val localizedMessage = when (item.type) {
            "LIKE" ->
                context.getString(R.string.notify_like_post)

            "LIKE_COMMENT" ->
                context.getString(
                    R.string.notify_like_comment,
                    item.content
                )

            "COMMENT" ->
                context.getString(
                    R.string.notify_comment,
                    item.content
                )

            "SHARE" ->
                context.getString(R.string.notify_share_post)

            "POST" ->
                context.getString(R.string.notify_post)

            "MESSAGE" ->
                context.getString(R.string.notify_message)

            "FOLLOW" ->
                context.getString(R.string.notify_follow)

            else -> null
        }

        // 👉 ƯU TIÊN MESSAGE ĐÃ LOCALIZE, FALLBACK MESSAGE CŨ
        val finalMessage = localizedMessage ?: item.message

        // ===== 2. HIỂN THỊ NỘI DUNG =====
        holder.tvContent.text = "${item.senderName} $finalMessage"

        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        holder.tvTime.text = sdf.format(item.timestamp.toDate())

        // ===== 3. HIỂN THỊ AVATAR =====
        Glide.with(context)
            .load(item.senderAvatar)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .circleCrop()
            .into(holder.ivAvatar)

        // ===== 4. ICON LOẠI NOTIFICATION =====
        val iconTypeRes = when(item.type) {
            "MESSAGE" -> R.drawable.ic_chat
            else -> R.drawable.ic_notifications
        }
        holder.ivType.setImageResource(iconTypeRes)
        // ===== 5. TRẠNG THÁI ĐỌC / CHƯA ĐỌC =====
        if (!item.isRead) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"))
            holder.tvContent.setTypeface(null, Typeface.BOLD)
            holder.ivUnreadDot.visibility = View.VISIBLE
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.tvContent.setTypeface(null, Typeface.NORMAL)
            holder.ivUnreadDot.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (!item.isRead) {
                item.isRead = true
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.white)
                )
                holder.tvContent.setTypeface(null, Typeface.NORMAL)
                holder.ivUnreadDot.visibility = View.GONE
            }
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