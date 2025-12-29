package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.UserRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

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
            }

            _isLoading.value = false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun updateUserProfile(user: User) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = userRepository.updateUser(user)

            if (result.isSuccess) {
                _user.value = user
            } else {
                val error = result.exceptionOrNull()
            }

            _isLoading.value = false
        }
    }
}