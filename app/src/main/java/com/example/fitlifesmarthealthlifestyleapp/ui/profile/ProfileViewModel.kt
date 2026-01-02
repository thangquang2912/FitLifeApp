package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.cloudinary.android.callback.UploadCallback

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        _isLoading.value = true;

        viewModelScope.launch {
            val result = userRepository.getUserDetails(uid)

            if (result.isSuccess) {
                _user.value = result.getOrNull()
            } else {
                val error = result.exceptionOrNull()
                error?.printStackTrace()
                _statusMessage.value = "Lỗi tải dữ liệu: ${result.exceptionOrNull()?.message}"
            }

            _isLoading.value = false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun saveUserProfile(user: User, imageUri: Uri?) {
        _isLoading.value = true

        // TH1: Không đổi ảnh -> Chỉ lưu thông tin text
        if (imageUri == null) {
            updateUserProfile(user)
            return
        }

        // TH2: Có ảnh -> Upload lên Cloudinary
        MediaManager.get().upload(imageUri)
            .option("folder", "fitlife_avatars")
            .option("public_id", user.uid)
            .option("overwrite", true)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) { }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // 1. Lấy link ảnh HTTPS
                    val secureUrl = resultData["secure_url"] as String

                    // 2. Cập nhật link vào User
                    user.photoUrl = secureUrl

                    // 3. Lưu User vào Firestore
                    updateUserProfile(user)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    _isLoading.postValue(false)
                    _statusMessage.postValue("Lỗi upload ảnh: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) { }
            })
            .dispatch()
    }

    fun updateUserProfile(user: User) {

        viewModelScope.launch {
            val result = userRepository.updateUser(user)

            if (result.isSuccess) {
                _user.value = user
                _statusMessage.value = "Updated profile successfully!" // Báo cho Fragment biết
            } else {
                _statusMessage.value = "Lỗi lưu dữ liệu: ${result.exceptionOrNull()?.message}"
            }

            _isLoading.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}