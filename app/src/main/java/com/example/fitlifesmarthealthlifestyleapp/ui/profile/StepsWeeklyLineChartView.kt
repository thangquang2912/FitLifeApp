package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.fitlifesmarthealthlifestyleapp.R
import kotlin.math.ceil

class StepsWeeklyLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0)
    // Danh sách nhãn ngày (Mon, Tue...) sẽ được cập nhật từ ViewModel
    private var labels: List<String> = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Today")
    private var maxSteps = 10000f
    
    private var animProgress = 0f
    
    // Paint vẽ đường biểu đồ
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.orange_primary)
    }

    // Paint vẽ vùng đổ bóng dưới đường (Area chart)
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // Paint vẽ các điểm nút (Dots)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val dotOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.orange_primary)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_grey)
        textSize = 10f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F0F0")
        strokeWidth = 1f * resources.displayMetrics.density
    }

    /**
     * Cập nhật danh sách nhãn ngày động từ ViewModel
     */
    fun setLabels(newLabels: List<String>) {
        this.labels = newLabels
        invalidate()
    }

    /**
     * Cập nhật dữ liệu cho biểu đồ và tính toán lại maxSteps
     */
    fun setWeeklyData(weeklySteps: List<Int>, goal: Int) {
        this.data = weeklySteps
        
        // Lấy số bước cao nhất trong tuần
        val highestInWeek = weeklySteps.maxOrNull() ?: 0

        val stepInterval = when {
            highestInWeek <= 3000 -> 500
            highestInWeek <= 7000 -> 1000
            highestInWeek <= 15000 -> 2000
            else -> 5000
        }

        // Lấy max giữa: highestInWeek & goal
        val rawMax = maxOf(highestInWeek, goal)

        this.maxSteps = ceil(rawMax / stepInterval.toFloat()) * stepInterval
        
        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val paddingBottom = 40f * resources.displayMetrics.density
        val paddingTop = 30f * resources.displayMetrics.density
        val paddingLeft = 45f * resources.displayMetrics.density
        val paddingRight = 20f * resources.displayMetrics.density
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val spacing = if (data.size > 1) chartWidth / (data.size - 1) else 0f

        // 1. Vẽ trục tọa độ Y (Số bước)
        for (i in 0..4) {
            val y = paddingTop + chartHeight - (chartHeight * i / 4)
            val value = (maxSteps * i / 4).toInt()
            // Chú thích tiếng Việt: Vẽ nhãn số lượng bên trái trục Y
            canvas.drawText(value.toString(), paddingLeft - 10f * resources.displayMetrics.density, y + 4f * resources.displayMetrics.density, textPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        val path = Path()
        val areaPath = Path()
        
        data.forEachIndexed { index, steps ->
            val x = paddingLeft + (index * spacing)
            val ratio = (steps / maxSteps).coerceAtMost(1f)
            val y = paddingTop + chartHeight - (chartHeight * ratio * animProgress)

            if (index == 0) {
                path.moveTo(x, y)
                areaPath.moveTo(x, paddingTop + chartHeight)
                areaPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
            
            if (index == data.size - 1) {
                areaPath.lineTo(x, paddingTop + chartHeight)
                areaPath.close()
            }

            // Vẽ nhãn thứ dưới trục X (Mon, Tue...)
            if (index < labels.size) {
                canvas.drawText(labels[index], x, height - 15f * resources.displayMetrics.density, textPaint.apply { textAlign = Paint.Align.CENTER })
            }
        }

        // 2. Vẽ vùng đổ màu Gradient (Vùng phía dưới đường kẻ)
        val colorOrange = ContextCompat.getColor(context, R.color.orange_primary)
        val gradient = LinearGradient(0f, paddingTop, 0f, paddingTop + chartHeight,
            colorOrange and 0x40FFFFFF, // Màu cam với độ trong suốt 25%
            Color.TRANSPARENT, Shader.TileMode.CLAMP)
        areaPaint.shader = gradient
        canvas.drawPath(areaPath, areaPaint)

        // 3. Vẽ đường biểu đồ chính
        canvas.drawPath(path, linePaint)

        // 4. Vẽ các điểm nút tại mỗi mốc dữ liệu
        data.forEachIndexed { index, steps ->
            val x = paddingLeft + (index * spacing)
            val ratio = (steps / maxSteps).coerceAtMost(1f)
            val y = paddingTop + chartHeight - (chartHeight * ratio * animProgress)
            
            canvas.drawCircle(x, y, 5f * resources.displayMetrics.density, dotPaint)
            canvas.drawCircle(x, y, 5f * resources.displayMetrics.density, dotOutlinePaint)
        }
    }
}
