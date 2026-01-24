package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitlifesmarthealthlifestyleapp.R
import kotlin.math.min

class MacroRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var carbsGram = 0f
    private var proteinGram = 0f
    private var fatGram = 0f

    // Cấu hình khoảng cách giữa các màu (Gap)
    // Khi dùng đầu vuông (BUTT), khe hở trông sẽ to hơn đầu tròn, nên mình giảm xuống 2 độ cho đẹp
    private val gapAngle = 2f

    private val strokeWidth = 15f * resources.displayMetrics.density // 15dp
    private val rectF = RectF()

    // --- BÚT VẼ (Đã sửa strokeCap thành BUTT) ---

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MacroRingView.strokeWidth
        strokeCap = Paint.Cap.BUTT // <--- SỬA THÀNH BUTT (Đầu vuông)
        color = ContextCompat.getColor(context, R.color.gray_text)
        alpha = 30
    }

    private val carbsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MacroRingView.strokeWidth
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.orange_primary)
    }

    private val proteinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MacroRingView.strokeWidth
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.chart_protein)
    }

    private val fatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MacroRingView.strokeWidth
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.chart_fat)
    }

    fun setMacros(carbs: Float, protein: Float, fat: Float, goal: Int) {
        this.carbsGram = carbs
        this.proteinGram = protein
        this.fatGram = fat
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val diameter = min(w, h) - strokeWidth
        val padding = strokeWidth / 2

        rectF.set(padding, padding, diameter + padding, diameter + padding)
        val offsetX = (w - (diameter + strokeWidth)) / 2
        val offsetY = (h - (diameter + strokeWidth)) / 2
        rectF.offset(offsetX, offsetY)

        val totalGrams = carbsGram + proteinGram + fatGram

        if (totalGrams <= 0) {
            // Trường hợp 1: Chưa ăn gì -> Vẽ 1 vòng tròn xám
            canvas.drawArc(rectF, 0f, 360f, false, emptyPaint)
        } else {
            // Trường hợp 2: Đã ăn -> Vẽ các màu chia nhau lấp đầy 360 độ
            val totalAngle = 360f

            // Tính góc cho từng phần
            val carbSweep = (carbsGram / totalGrams) * totalAngle
            val proteinSweep = (proteinGram / totalGrams) * totalAngle
            val fatSweep = (fatGram / totalGrams) * totalAngle

            // Bắt đầu từ đỉnh 12h (-90 độ)
            var currentStartAngle = -90f

            // --- VẼ CARBS ---
            if (carbSweep > 0) {
                // Trừ đi gapAngle để tạo khe hở
                val drawSweep = (carbSweep - gapAngle).coerceAtLeast(0.5f)
                canvas.drawArc(rectF, currentStartAngle, drawSweep, false, carbsPaint)
                currentStartAngle += carbSweep
            }

            // --- VẼ PROTEIN ---
            if (proteinSweep > 0) {
                val drawSweep = (proteinSweep - gapAngle).coerceAtLeast(0.5f)
                canvas.drawArc(rectF, currentStartAngle, drawSweep, false, proteinPaint)
                currentStartAngle += proteinSweep
            }

            // --- VẼ FAT ---
            if (fatSweep > 0) {
                val drawSweep = (fatSweep - gapAngle).coerceAtLeast(0.5f)
                canvas.drawArc(rectF, currentStartAngle, drawSweep, false, fatPaint)
                // Không cần cộng currentStartAngle nữa
            }
        }
    }
}