package com.example.fitlifesmarthealthlifestyleapp.ui.auth

sealed class AuthState {
    object Idle : AuthState()          // Trạng thái chờ, chưa làm gì
    object Loading : AuthState()       // Đang quay vòng tròn load
    object Success : AuthState()       // Đăng nhập/Đăng ký thành công
    data class Error(val message: String) : AuthState() // Có lỗi xảy ra
}