package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.DeepLinkViewModel
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationFragment : Fragment(R.layout.fragment_notifications) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var adapter: NotificationAdapter
    private lateinit var deepLinkViewModel: DeepLinkViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Setup Adapter
        adapter = NotificationAdapter(emptyList()) { notif ->
            handleNotificationClick(notif)
        }

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        // Tải dữ liệu từ Firebase
        listenToNotifications()
    }

    private fun listenToNotifications() {
        if (currentUid == null) return

        // Khi SnapshotListener trả về, model Notification đã có sẵn biến isRead từ Firebase
        db.collection("users").document(currentUid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Notification::class.java) ?: emptyList()

                // Adapter sẽ dựa vào danh sách này để render màu nền đúng ngay từ đầu
                adapter.updateList(list)
            }
    }

    private fun handleNotificationClick(notif: Notification) {
        if (currentUid == null) return

        // 1. CẬP NHẬT TRẠNG THÁI LÊN FIREBASE
        db.collection("users").document(currentUid)
            .collection("notifications").document(notif.id)
            .update("isRead", true, "read", true)
            .addOnFailureListener {
                Log.e("NotifError", "Không thể update isRead: ${it.message}")
            }

        // 2. XỬ LÝ ĐIỀU HƯỚNG THEO CÁCH CỦA BẠN
        if (notif.postId.isNotEmpty()) {
            // Gán ID bài viết vào ViewModel để SocialFragment bắt được
            deepLinkViewModel.setPostId(notif.postId)

            // Quay lại màn hình trước đó (SocialFragment)
            parentFragmentManager.popBackStack()

        } else if (notif.type == "MESSAGE") {
            // Đối với tin nhắn, thường mình sẽ chuyển sang màn hình Chat mới
            // Nếu bạn muốn Chat cũng pop rồi mới chuyển thì làm tương tự:
            parentFragmentManager.popBackStack()

            parentFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, CommunityChatFragment())
                .addToBackStack(null)
                .commit()
        } else {
            // Các thông báo khác (như Follow) chỉ cần pop để về lại trang trước
            parentFragmentManager.popBackStack()
        }
    }
}