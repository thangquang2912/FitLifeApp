package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.fitlifesmarthealthlifestyleapp.R
import java.util.*

class WaterWeeklyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dữ liệu lượng nước uống trong 7 ngày
    private var data: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0)
    private var labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private var maxGoal = 2000f
    
    // Biến điều khiển animation (0.0 -> 1.0)
    private var animProgress = 0f
    
    // Paint để vẽ các cột (bars)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // Paint để vẽ chữ (labels)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_grey)
        textSize = 12f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    // Paint để vẽ các đường kẻ nền (grid lines)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F0F0")
        strokeWidth = 1f * resources.displayMetrics.density
    }

    /**
     * Cập nhật dữ liệu cho biểu đồ và chạy animation
     * @param weeklyIntakes Danh sách lượng nước uống trong 7 ngày
     * @param goal Mục tiêu uống nước hàng ngày để tính tỷ lệ cột
     */
    fun setWeeklyData(weeklyIntakes: List<Int>, goal: Int) {
        this.data = weeklyIntakes
        this.maxGoal = goal.toFloat().coerceAtLeast(1000f)
        startAnimation()
    }

    /**
     * Khởi chạy animation từ 0 đến 1 cho các cột
     */
    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000 // Chạy trong 1 giây
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate() // Vẽ lại View khi giá trị animation thay đổi
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        // Tính toán các khoảng cách padding
        val paddingBottom = 30f * resources.displayMetrics.density
        val paddingTop = 20f * resources.displayMetrics.density
        val paddingLeft = 16f * resources.displayMetrics.density
        val paddingRight = 16f * resources.displayMetrics.density
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        // Độ rộng mỗi cột (chiếm 50% không gian phân bổ)
        val barWidth = (chartWidth / data.size) * 0.5f
        val spacing = (chartWidth / data.size)

        // Vẽ các đường kẻ ngang mờ (0%, 50%, 100% mục tiêu)
        for (i in 0..2) {
            val y = paddingTop + chartHeight - (chartHeight * i / 2)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        data.forEachIndexed { index, intake ->
            val x = paddingLeft + (index * spacing) + (spacing / 2)
            
            // Vẽ nhãn ngày (Thứ 2, Thứ 3...) bên dưới cột
            canvas.drawText(labels[index], x, height - 10f * resources.displayMetrics.density, textPaint)

            // Tính toán chiều cao cột dựa trên lượng nước và mục tiêu
            val ratio = (intake / maxGoal).coerceAtMost(1.2f) // Giới hạn cột không quá cao
            val barHeight = chartHeight * ratio * animProgress
            
            val top = paddingTop + chartHeight - barHeight
            val bottom = paddingTop + chartHeight
            val left = x - (barWidth / 2)
            val right = x + (barWidth / 2)

            val rect = RectF(left, top, right, bottom)
            
            // Đổ màu Gradient cho cột từ đậm (dưới) sang nhạt (trên)
            val colorStart = ContextCompat.getColor(context, R.color.blue_primary)
            val colorEnd = ContextCompat.getColor(context, R.color.blue_light)
            barPaint.shader = LinearGradient(0f, top, 0f, bottom, colorStart, colorEnd, Shader.TileMode.CLAMP)
            
            // Vẽ cột với góc bo tròn 8dp
            canvas.drawRoundRect(rect, 8f * resources.displayMetrics.density, 8f * resources.displayMetrics.density, barPaint)
        }
    }
}
