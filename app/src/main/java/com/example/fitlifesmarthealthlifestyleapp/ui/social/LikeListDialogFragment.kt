package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.fitlifesmarthealthlifestyleapp.R // Import R để dùng ID container
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikeListDialogFragment(private val userIds: List<String>) : DialogFragment() {

    private val userRepository = UserRepository()
    private val userList = ArrayList<User>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // [MỚI] Truyền callback điều hướng vào Adapter
        val adapter = LikesAdapter(requireContext(), userList) { targetUserId ->
            // Đóng dialog trước khi chuyển màn hình
            dismiss()

            // Logic chuyển màn hình giống các trang khác
            val fragment = if (targetUserId == currentUid) {
                PersonalProfileFragment()
            } else {
                UserProfileFragment.newInstance(targetUserId)
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navHostFragmentContainerView, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Fetch thông tin User (Giữ nguyên logic cũ)
        CoroutineScope(Dispatchers.Main).launch {
            userIds.forEach { uid ->
                val result = withContext(Dispatchers.IO) {
                    userRepository.getUserDetails(uid)
                }
                result.getOrNull()?.let { user ->
                    userList.add(user)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Likes")
            .setAdapter(adapter, null)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
    }
}