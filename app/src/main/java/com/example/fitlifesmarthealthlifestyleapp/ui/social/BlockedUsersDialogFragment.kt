package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BlockedUsersDialogFragment : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    private val blockedUsersList = mutableListOf<User>()
    private lateinit var adapter: BlockedUsersAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val view = layoutInflater.inflate(R.layout.dialog_list_simple, null) // Tận dụng layout có sẵn hoặc tạo mới chỉ có RecyclerView
        // Hoặc tạo layout dialog_blocked_users.xml chỉ chứa RecyclerView

        val rv = RecyclerView(requireContext())
        rv.layoutManager = LinearLayoutManager(context)
        rv.setPadding(0, 20, 0, 20)

        adapter = BlockedUsersAdapter(blockedUsersList) { userToUnblock ->
            unblockUser(userToUnblock)
        }
        rv.adapter = adapter

        fetchBlockedUsers()

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_blocked_users))
            .setView(rv)
            .setPositiveButton(getString(R.string.btn_close), null)
            .create()
    }

    private fun fetchBlockedUsers() {
        if (currentUid == null) return

        // 1. Lấy danh sách ID bị chặn
        db.collection("users").document(currentUid).get().addOnSuccessListener { doc ->
            val blockedIds = doc.get("blockedUsers") as? List<String> ?: emptyList()

            if (blockedIds.isEmpty()) {
                Toast.makeText(
                    context,
                    getString(R.string.msg_no_blocked_users),
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            // 2. Load thông tin chi tiết từng User
            blockedUsersList.clear()
            // (Cách đơn giản: loop load. Cách tối ưu: whereIn nếu < 10 items)
            blockedIds.forEach { uid ->
                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        blockedUsersList.add(user)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun unblockUser(user: User) {
        if (currentUid == null) return

        val batch = db.batch()
        val myRef = db.collection("users").document(currentUid)
        batch.update(myRef, "blockedUsers", FieldValue.arrayRemove(user.uid))
        val targetRef = db.collection("users").document(user.uid)
        batch.update(targetRef, "blockedBy", FieldValue.arrayRemove(currentUid))

        batch.commit()
            .addOnSuccessListener {
                // [FIX] Check an toàn trước khi dùng context/adapter
                if (!isAdded || context == null) return@addOnSuccessListener

                Toast.makeText(
                    context,
                    getString(R.string.msg_unblock_success, user.displayName),
                    Toast.LENGTH_SHORT
                ).show()
                blockedUsersList.remove(user)
                adapter.notifyDataSetChanged()

                if (blockedUsersList.isEmpty()) {
                    Toast.makeText(
                        context,
                        getString(R.string.msg_no_blocked_users),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(
                        context,
                        getString(R.string.msg_unblock_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}