package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiModerator {

    // Thay thế bằng API KEY của bạn

    private val apiKey = BuildConfig.API_KEY_GEMINI
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    suspend fun isContentSafe(context: Context, imageUri: Uri?, caption: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Chuẩn bị ảnh
                var bitmap: Bitmap? = null
                if (imageUri != null) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    bitmap = BitmapFactory.decodeStream(inputStream)
                }

                val prompt = """
                    Vai trò: Bạn là Kiểm duyệt viên nội dung nghiêm khắc.
                    Văn bản: "$caption"
                    Yêu cầu: Nếu vi phạm (tục tĩu, khiêu dâm, bạo lực, thù địch) trả lời "UNSAFE". Nếu sạch trả lời "SAFE".
                """.trimIndent()

                val inputContent = content {
                    if (bitmap != null) image(bitmap)
                    text(prompt)
                }

                // Gửi request
                val response = generativeModel.generateContent(inputContent)
                val resultText = response.text?.trim()?.uppercase()

                // --- LOG KẾT QUẢ RA LOGCAT ĐỂ XEM ---
                Log.d("GEMINI_CHECK", "--------------------------------")
                Log.d("GEMINI_CHECK", "Input Caption: $caption")
                Log.d("GEMINI_CHECK", "Has Image: ${bitmap != null}")
                Log.d("GEMINI_CHECK", "AI Response: $resultText") // Xem nó trả về SAFE, UNSAFE hay null
                Log.d("GEMINI_CHECK", "--------------------------------")

                if (resultText == null) {
                    Log.e("GEMINI_CHECK", "AI Blocked Response (Nội dung quá độc hại)")
                    return@withContext false
                }

                return@withContext resultText == "SAFE"

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("GEMINI_CHECK", "Lỗi API: ${e.message}")

                if (e.message?.contains("safety", ignoreCase = true) == true ||
                    e.message?.contains("blocked", ignoreCase = true) == true) {
                    Log.e("GEMINI_CHECK", "Bị chặn bởi Safety Filter của Google")
                    return@withContext false
                }

                return@withContext true
            }
        }
    }
}