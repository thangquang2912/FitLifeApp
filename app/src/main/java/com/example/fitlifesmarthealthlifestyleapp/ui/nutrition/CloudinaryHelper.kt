package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.net.Uri
import android.util.Log // 1. Import Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper" // Tag để lọc trong Logcat

    // Hàm upload ảnh lên thư mục "fitlife_meals"
    suspend fun uploadImage(imageUri: Uri, folderName: String = "fitlife_meals"): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // MediaManager đã được init trong MyApplication
                MediaManager.get().upload(imageUri)
                    .option("folder", folderName)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {
                            Log.d(TAG, "Bắt đầu upload...")
                        }

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                            // Có thể log tiến độ nếu thích
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            // Lấy link ảnh HTTPS thành công
                            val secureUrl = resultData?.get("secure_url") as? String

                            // 2. IN LINK ẢNH RA LOGCAT ĐỂ BẠN CLICK XEM
                            Log.d(TAG, "Upload THÀNH CÔNG! Link ảnh: $secureUrl")

                            continuation.resume(secureUrl)
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            // 3. SỬA LỖI TẠI ĐÂY: Dùng Log.e thay vì printStackTrace
                            val msg = error?.description ?: "Lỗi không xác định"
                            Log.e(TAG, "Upload THẤT BẠI: $msg")

                            continuation.resume(null) // Lỗi trả về null
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            continuation.resume(null)
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                e.printStackTrace() // Exception thật của Java thì dùng cái này được
                continuation.resume(null)
            }
        }
    }
}