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
    private val onImageClick: (String) -> Unit, // [MỚI] Callback xem ảnh full
    private val onUserClick: (String) -> Unit   // [MỚI] Callback xem profile
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    fun updateList(newList: List<CommunityMessage>) {
        messages = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUid) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_msg_me, parent, false)
            MeViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_msg_other, parent, false)
            OtherViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is MeViewHolder) holder.bind(msg)
        else if (holder is OtherViewHolder) holder.bind(msg)
    }

    override fun getItemCount() = messages.size

    private fun formatTime(timestamp: com.google.firebase.Timestamp): String {
        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    // --- VIEWHOLDER CỦA TÔI ---
    inner class MeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvMsgText)
        val ivImage: ImageView = itemView.findViewById(R.id.ivMsgImage)
        val tvTime: TextView = itemView.findViewById(R.id.tvMsgTime)

        fun bind(msg: CommunityMessage) {
            tvTime.text = formatTime(msg.timestamp)

            if (msg.type == "IMAGE") {
                tvText.visibility = View.GONE
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(msg.imageUrl).into(ivImage)

                // [MỚI] Click vào ảnh -> Xem Full Screen
                ivImage.setOnClickListener {
                    if (msg.imageUrl.isNotEmpty()) onImageClick(msg.imageUrl)
                }
            } else {
                tvText.visibility = View.VISIBLE
                ivImage.visibility = View.GONE
                tvText.text = msg.text
            }

            itemView.setOnLongClickListener {
                onMessageLongClick(msg, itemView)
                true
            }
        }
    }

    // --- VIEWHOLDER NGƯỜI KHÁC ---
    inner class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvMsgText)
        val ivImage: ImageView = itemView.findViewById(R.id.ivMsgImage)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvSenderName)
        val tvTime: TextView = itemView.findViewById(R.id.tvMsgTime)

        fun bind(msg: CommunityMessage) {
            tvName.text = msg.senderName
            tvTime.text = formatTime(msg.timestamp)

            Glide.with(itemView.context).load(msg.senderAvatar).placeholder(R.drawable.ic_user).circleCrop().into(ivAvatar)

            // [MỚI] Click Avatar hoặc Tên -> Xem Profile
            val openProfile = View.OnClickListener { onUserClick(msg.senderId) }
            ivAvatar.setOnClickListener(openProfile)
            tvName.setOnClickListener(openProfile)

            if (msg.type == "IMAGE") {
                tvText.visibility = View.GONE
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(msg.imageUrl).into(ivImage)

                // [MỚI] Click vào ảnh -> Xem Full Screen
                ivImage.setOnClickListener {
                    if (msg.imageUrl.isNotEmpty()) onImageClick(msg.imageUrl)
                }
            } else {
                tvText.visibility = View.VISIBLE
                ivImage.visibility = View.GONE
                tvText.text = msg.text
            }
        }
    }
}