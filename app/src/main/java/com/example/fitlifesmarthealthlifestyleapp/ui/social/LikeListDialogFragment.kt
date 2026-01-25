package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikeListDialogFragment(private val userIds: List<String>) : DialogFragment() {

    private val userRepository = UserRepository()
    private val userList = ArrayList<User>() // Danh sách chứa User object

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Sử dụng Adapter tùy chỉnh vừa tạo
        val adapter = LikesAdapter(requireContext(), userList)

        // Fetch thông tin User
        CoroutineScope(Dispatchers.Main).launch {
            userIds.forEach { uid ->
                // Chạy ngầm để lấy dữ liệu
                val result = withContext(Dispatchers.IO) {
                    userRepository.getUserDetails(uid)
                }

                // Cập nhật giao diện khi có dữ liệu
                result.getOrNull()?.let { user ->
                    userList.add(user)
                    adapter.notifyDataSetChanged() // Báo cho list cập nhật dòng mới
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Likes") // Tiêu đề
            .setAdapter(adapter, null) // Gán Adapter tùy chỉnh vào
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
    }
}