package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore

// Adapter đơn giản cho List User
class SimpleUserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<SimpleUserAdapter.UserViewHolder>() {

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_user, parent, false) // Tận dụng layout item có sẵn
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.displayName
        Glide.with(holder.itemView).load(user.photoUrl).circleCrop().placeholder(R.drawable.ic_user).into(holder.ivAvatar)
        holder.itemView.setOnClickListener { onUserClick(user) }

        // Ẩn nút unblock nếu dùng lại layout item_blocked_user
        holder.btnUnblock.visibility = View.GONE
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivBlockedAvatar)
        val tvName: TextView = view.findViewById(R.id.tvBlockedName)
        val btnUnblock: View = view.findViewById(R.id.btnUnblock)
    }
}

class UserListDialogFragment(
    private val title: String,
    private val userIds: List<String>
) : BottomSheetDialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private val loadedUsers = mutableListOf<User>()
    private lateinit var adapter: SimpleUserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_user_list_search, container, false) // Bạn cần tạo layout này
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val searchView = view.findViewById<SearchView>(R.id.svUserList)
        val rv = view.findViewById<RecyclerView>(R.id.rvUserList)

        adapter = SimpleUserAdapter(emptyList()) { user ->
            // Khi bấm vào user trong list -> mở profile người đó
            val fragment = if (user.uid == com.google.firebase.auth.FirebaseAuth.getInstance().uid)
                PersonalProfileFragment() else UserProfileFragment.newInstance(user.uid)

            parentFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, fragment)
                .addToBackStack(null)
                .commit()
            dismiss()
        }
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        loadUsers()

        // Tìm kiếm
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.lowercase() ?: ""
                val filtered = loadedUsers.filter { it.displayName.lowercase().contains(query) }
                adapter.updateList(filtered)
                return true
            }
        })
    }

    private fun loadUsers() {
        if (userIds.isEmpty()) return
        // Lưu ý: Firestore 'in' query giới hạn 10 item. Nếu list dài cần chia nhỏ logic.
        // Ở đây làm đơn giản: fetch từng người (hoặc tối ưu sau)
        userIds.forEach { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    loadedUsers.add(user)
                    adapter.updateList(loadedUsers)
                }
            }
        }
    }
}