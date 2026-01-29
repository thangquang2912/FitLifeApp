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

    // API Key từ BuildConfig
    private val apiKey = BuildConfig.API_KEY_GEMINI

    // Sử dụng model mới nhất
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // Hoặc gemini-1.5-flash tùy key của bạn
        apiKey = apiKey
    )

    suspend fun isContentSafe(context: Context, imageUri: Uri?, caption: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Chuẩn bị ảnh (Nếu có)
                var bitmap: Bitmap? = null
                if (imageUri != null) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    } catch (e: Exception) {
                        Log.e("GEMINI", "Lỗi đọc ảnh: ${e.message}")
                    }
                }

                // 2. Prompt (Lệnh) - CỰC KỲ NGHIÊM KHẮC VỚI TIẾNG VIỆT
                val prompt = """
                    Bạn là hệ thống kiểm duyệt nội dung Tiếng Việt (Vietnam).
                    Nhiệm vụ: Phân loại xem nội dung sau có An toàn hay Vi phạm.
                    
                    Định nghĩa VI PHẠM (UNSAFE):
                    1. Tục tĩu, chửi thề (kể cả viết tắt, viết lái, teencode như: vc, vcl, dm, dcm, cac, cc, ngu, chó...).
                    2. Thù địch, xúc phạm người khác.
                    3. Khiêu dâm, bạo lực.
                    
                    Nội dung cần kiểm tra: "$caption"
                    
                    Yêu cầu bắt buộc:
                    - Chỉ trả lời duy nhất một từ: "SAFE" hoặc "UNSAFE".
                    - Nếu nghi ngờ hoặc không chắc chắn, hãy trả lời "UNSAFE".
                    - Không giải thích gì thêm.
                """.trimIndent()

                val inputContent = content {
                    if (bitmap != null) image(bitmap)
                    text(prompt)
                }

                // 3. Gửi Request
                val response = generativeModel.generateContent(inputContent)

                // Kiểm tra nếu bị Google chặn ngay từ đầu (Safety Filters)
                if (response.candidates.isEmpty()) {
                    Log.e("GEMINI", "Bị chặn bởi bộ lọc Google (Empty Candidates)")
                    return@withContext false // Coi là UNSAFE
                }

                // Lấy kết quả và chuẩn hóa
                val resultText = response.text?.trim()?.uppercase() ?: ""
                Log.d("GEMINI", "AI Trả lời: $resultText")

                // 4. Logic kiểm tra kết quả chặt chẽ hơn
                // - Nếu AI trả lời "UNSAFE" -> Chặn ngay.
                // - Nếu AI trả lời dài dòng mà có chứa chữ "UNSAFE" -> Chặn ngay.
                // - Chỉ khi AI trả lời CHÍNH XÁC là "SAFE" hoặc chứa "SAFE" mà KHÔNG có "UNSAFE" mới cho qua.

                if (resultText.contains("UNSAFE")) {
                    return@withContext false
                }

                if (resultText.contains("SAFE")) {
                    return@withContext true
                }

                // Trường hợp AI trả lời linh tinh không đúng định dạng -> Chặn cho chắc
                return@withContext false

            } catch (e: Exception) {
                val msg = e.message?.lowercase() ?: ""
                Log.e("GEMINI", "Lỗi API: $msg")

                // Nếu lỗi do bộ lọc an toàn của Google chặn (Finish Reason: SAFETY)
                if (msg.contains("safety") || msg.contains("blocked") || msg.contains("finish reason")) {
                    Log.e("GEMINI", "Nội dung bị chặn bởi Google Safety API")
                    return@withContext false
                }

                // Các lỗi khác (Mạng, Key sai...) -> Tạm thời cho qua để không ảnh hưởng trải nghiệm
                // (Hoặc return false nếu bạn muốn bảo mật tuyệt đối)
                return@withContext true
            }
        }
    }
}