package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.DeepLinkViewModel
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationFragment : Fragment(R.layout.fragment_notifications) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var adapter: NotificationAdapter
    private lateinit var deepLinkViewModel: DeepLinkViewModel

    // Hàm tiện ích để hiện lại BottomNav (đề phòng trường hợp bị ẩn trước đó)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        adapter = NotificationAdapter(emptyList()) { notif ->
            handleNotificationClick(notif)
        }

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        listenToNotifications()
    }

    private fun listenToNotifications() {
        if (currentUid == null) return
        db.collection("users").document(currentUid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                adapter.updateList(list)
            }
    }

    private fun handleNotificationClick(notif: Notification) {
        if (currentUid == null) return

        // 1. Cập nhật Firebase ngay lập tức
        db.collection("users").document(currentUid)
            .collection("notifications").document(notif.id)
            .update("isRead", true)

        // 2. Điều hướng
        if (notif.postId.isNotEmpty()) {
            deepLinkViewModel.setPostId(notif.postId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, SocialFragment())
                .addToBackStack(null)
                .commit()
        } else if (notif.type == "MESSAGE") {
            // Chuyển sang Chat
            parentFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, CommunityChatFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}