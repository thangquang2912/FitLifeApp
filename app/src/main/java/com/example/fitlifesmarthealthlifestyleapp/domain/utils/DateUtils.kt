package com.example.fitlifesmarthealthlifestyleapp.domain.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    // ID Document sẽ có dạng: "2025-12-30"
    fun getCurrentDateId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}