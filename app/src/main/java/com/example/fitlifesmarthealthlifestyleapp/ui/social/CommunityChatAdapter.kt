package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.CommunityMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityChatAdapter(
    private var messages: List<CommunityMessage>,
    private val onMessageLongClick: (CommunityMessage, View) -> Unit,
    private val onImageClick: (String) -> Unit,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    fun updateList(newList: List<CommunityMessage>) {
        messages = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].senderId == currentUid) TYPE_ME else TYPE_OTHER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_ME) R.layout.item_community_msg_me else R.layout.item_community_msg_other
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == TYPE_ME) MeViewHolder(view) else OtherViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is MeViewHolder) holder.bind(msg) else if (holder is OtherViewHolder) holder.bind(msg)
    }

    override fun getItemCount() = messages.size

    // SỬA LỖI: Thêm 'val v: View'
    inner class MeViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(msg: CommunityMessage) {
            val tv = v.findViewById<TextView>(R.id.tvMsgText)
            val iv = v.findViewById<ImageView>(R.id.ivMsgImage)
            v.findViewById<TextView>(R.id.tvMsgTime).text = formatTime(msg.timestamp)

            // Logic gộp nội dung: Hiển thị cả text và image nếu có
            tv.text = msg.text
            tv.visibility = if (msg.text.isNotEmpty()) View.VISIBLE else View.GONE

            if (msg.imageUrl.isNotEmpty()) {
                iv.visibility = View.VISIBLE
                Glide.with(v.context).load(msg.imageUrl).into(iv)
                iv.setOnClickListener { onImageClick(msg.imageUrl) }
                iv.setOnLongClickListener { onMessageLongClick(msg, it); true }
            } else {
                iv.visibility = View.GONE
            }
            v.setOnLongClickListener { onMessageLongClick(msg, it); true }
        }
    }

    inner class OtherViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(msg: CommunityMessage) {
            v.findViewById<TextView>(R.id.tvSenderName).text = msg.senderName
            v.findViewById<TextView>(R.id.tvMsgTime).text = formatTime(msg.timestamp)
            val ivAvatar = v.findViewById<ImageView>(R.id.ivAvatar)

            Glide.with(v.context).load(msg.senderAvatar).placeholder(R.drawable.ic_user).circleCrop().into(ivAvatar)
            ivAvatar.setOnClickListener { onUserClick(msg.senderId) }

            val tv = v.findViewById<TextView>(R.id.tvMsgText)
            val iv = v.findViewById<ImageView>(R.id.ivMsgImage)

            tv.text = msg.text
            tv.visibility = if (msg.text.isNotEmpty()) View.VISIBLE else View.GONE

            if (msg.imageUrl.isNotEmpty()) {
                iv.visibility = View.VISIBLE
                Glide.with(v.context).load(msg.imageUrl).into(iv)
                iv.setOnClickListener { onImageClick(msg.imageUrl) }
                iv.setOnLongClickListener { onMessageLongClick(msg, it); true }
            } else {
                iv.visibility = View.GONE
            }
            v.setOnLongClickListener { onMessageLongClick(msg, it); true }
        }
    }

    private fun formatTime(ts: com.google.firebase.Timestamp?): String {
        if (ts == null) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate())
    }
}