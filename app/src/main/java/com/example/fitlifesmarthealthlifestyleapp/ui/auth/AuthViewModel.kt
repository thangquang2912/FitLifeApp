package com.example.fitlifesmarthealthlifestyleapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    // Hàm Đăng nhập
    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = repository.signIn(email, pass)
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                val exception = result.exceptionOrNull()

                // Phân loại lỗi
                val errorMsg = when (exception) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                    is FirebaseNetworkException -> "Network error"
                    else -> exception?.message ?: "Login Failed"
                }
                _authState.value = AuthState.Error(errorMsg)
            }
        }
    }

    fun loginGoogle(idToken: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }

    // Hàm Đăng ký
    fun register(fullName: String, email: String, pass: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = repository.signUp(fullName, email, pass)
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Đăng ký thất bại"
                _authState.value = AuthState.Error(errorMsg)
            }
        }
    }

    // Kiểm tra đã đăng nhập chưa
    fun checkLoginStatus(): Boolean {
        return repository.isSignedIn()
    }
}