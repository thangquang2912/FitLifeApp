package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User

class LikesAdapter(
    context: Context,
    private val users: ArrayList<User>,
    private val onUserClick: (String) -> Unit // [MỚI] Callback truyền User ID ra ngoài
) : ArrayAdapter<User>(context, 0, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.item_user_like, parent, false)
        }

        val user = getItem(position)
        val ivAvatar = itemView!!.findViewById<ImageView>(R.id.ivAvatarLike)
        val tvName = itemView.findViewById<TextView>(R.id.tvNameLike)

        if (user != null) {
            tvName.text = user.displayName

            Glide.with(context)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_user)
                .circleCrop()
                .into(ivAvatar)

            // [MỚI] Gắn sự kiện click vào toàn bộ item (hoặc chỉ avatar/tên tùy bạn)
            itemView.setOnClickListener {
                onUserClick(user.uid)
            }
        }

        return itemView
    }
}