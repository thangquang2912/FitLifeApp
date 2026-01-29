package com.example.fitlifesmarthealthlifestyleapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeepLinkViewModel : ViewModel() {
    // Biến lưu ID bài viết (LiveData để các màn hình tự lắng nghe)
    val targetPostId = MutableLiveData<String?>()

    // 1. Hàm lưu ID (Dùng ở MainActivity)
    fun setPostId(id: String) {
        targetPostId.value = id
    }

    // 2. Hàm xóa ID (Dùng ở SocialFragment sau khi đã lấy xong)
    fun clearPostId() {
        targetPostId.value = null
    }
}