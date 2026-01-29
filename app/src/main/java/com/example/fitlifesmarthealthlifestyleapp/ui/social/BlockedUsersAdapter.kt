package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User

class BlockedUsersAdapter(
    private val users: MutableList<User>,
    private val onUnblockClick: (User) -> Unit
) : RecyclerView.Adapter<BlockedUsersAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivBlockedAvatar)
        val tvName: TextView = view.findViewById(R.id.tvBlockedName)
        val btnUnblock: Button = view.findViewById(R.id.btnUnblock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.displayName
        Glide.with(holder.itemView).load(user.photoUrl).circleCrop().placeholder(R.drawable.ic_user).into(holder.ivAvatar)

        holder.btnUnblock.setOnClickListener {
            onUnblockClick(user)
        }
    }

    override fun getItemCount() = users.size
}