package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.util.Patterns
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SimpleUserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<SimpleUserAdapter.UserViewHolder>() {

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val currentUid = FirebaseAuth.getInstance().uid

        // Hiển thị "Bạn" nếu là chính mình trong danh sách
        holder.tvName.text = if (user.uid == currentUid) "Bạn" else user.displayName

        Glide.with(holder.itemView.context)
            .load(user.photoUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_user)
            .into(holder.ivAvatar)

        holder.itemView.setOnClickListener { onUserClick(user) }
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
        return inflater.inflate(R.layout.dialog_user_list_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val searchView = view.findViewById<SearchView>(R.id.svUserList)
        val rv = view.findViewById<RecyclerView>(R.id.rvUserList)

        adapter = SimpleUserAdapter(emptyList()) { user ->
            val currentUid = FirebaseAuth.getInstance().uid
            val fragment = if (user.uid == currentUid) PersonalProfileFragment() else UserProfileFragment.newInstance(user.uid)

            // Fix lỗi crash: Sử dụng Activity manager
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, fragment)
                .addToBackStack(null)
                .commit()
            dismiss()
        }

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter
        loadUsers()

        // --- LOGIC SEARCH THEO EMAIL VÀ TÊN ---
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.lowercase()?.trim() ?: ""

                if (query.isEmpty()) {
                    adapter.updateList(loadedUsers)
                    return true
                }

                val filtered = if (Patterns.EMAIL_ADDRESS.matcher(query).matches()) {
                    // Nếu là định dạng Email: Tìm khớp chính xác trường email
                    loadedUsers.filter { it.email.lowercase() == query }
                } else {
                    // Nếu là văn bản: Tìm theo tên hiển thị (như cũ)
                    loadedUsers.filter { it.displayName.lowercase().contains(query) }
                }

                adapter.updateList(filtered)
                return true
            }
        })
    }

    private fun loadUsers() {
        if (userIds.isEmpty()) return
        userIds.forEach { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null && isAdded) {
                    loadedUsers.add(user)
                    //distinctBy để tránh trùng lặp dữ liệu khi listener chạy lại
                    adapter.updateList(loadedUsers.distinctBy { it.uid })
                }
            }
        }
    }
}