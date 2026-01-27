package com.example.fitlifesmarthealthlifestyleapp.utils // Tạo package utils nếu chưa có

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    // Hàm tiện ích vừa kiểm tra vừa báo lỗi
    fun checkConnection(context: Context): Boolean {
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, "Không có kết nối Internet!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}