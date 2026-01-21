package com.example.fitlifesmarthealthlifestyleapp.ui.social

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.fitlifesmarthealthlifestyleapp.R
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageDialogFragment : DialogFragment(R.layout.dialog_fullscreen_image) {

    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // QUAN TRỌNG: Bỏ style Fullscreen cũ đi để có thể chỉnh size
        // setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen) -> XÓA DÒNG NÀY
        imageUrl = arguments?.getString("imageUrl")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoView = view.findViewById<PhotoView>(R.id.photoViewFullScreen)
        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseFullScreen)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarFullScreen)

        btnClose.setOnClickListener { dismiss() }

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(photoView)
        } else {
            progressBar.visibility = View.GONE
        }
    }

    // CẤU HÌNH KÍCH THƯỚC 95% TẠI ĐÂY
    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt() // 95% Chiều rộng
            val height = (resources.displayMetrics.heightPixels * 0.85).toInt() // 85% Chiều cao (cho cân đối hơn với ảnh)

            dialog.window?.setLayout(width, height)

            // Làm trong suốt nền gốc của Android để thấy được bo góc của XML
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager, imageUrl: String) {
            val dialog = FullScreenImageDialogFragment()
            dialog.arguments = Bundle().apply {
                putString("imageUrl", imageUrl)
            }
            dialog.show(fragmentManager, "FullScreenImage")
        }
    }
}