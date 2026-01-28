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

    private var data: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0)
    private var labels: List<String> = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Today")
    private var maxGoal = 2000f
    
    private var animProgress = 0f
    
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
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

    fun setLabels(newLabels: List<String>) {
        this.labels = newLabels
        invalidate()
    }

    fun setWeeklyData(weeklyIntakes: List<Int>, goal: Int) {
        this.data = weeklyIntakes
        this.maxGoal = weeklyIntakes.maxOrNull()?.toFloat()?.coerceAtLeast(goal.toFloat()) ?: goal.toFloat()
        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
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
        
        val barWidth = (chartWidth / data.size) * 0.5f
        val spacing = (chartWidth / data.size)

        for (i in 0..4) {
            val y = paddingTop + chartHeight - (chartHeight * i / 4)
            val value = (maxGoal * i / 4).toInt()
            canvas.drawText("${value}ml", paddingLeft - 10f * resources.displayMetrics.density, y + 4f * resources.displayMetrics.density, textPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        data.forEachIndexed { index, intake ->
            val x = paddingLeft + (index * spacing) + (spacing / 2)
            
            if (index < labels.size) {
                canvas.drawText(labels[index], x, height - 15f * resources.displayMetrics.density, textPaint.apply { textAlign = Paint.Align.CENTER })
            }

            val ratio = (intake / maxGoal).coerceAtMost(1f)
            val barHeight = chartHeight * ratio * animProgress
            
            // CHỈ VẼ KHI CHIỀU CAO CỘT > 0 để tránh lỗi LinearGradient crash
            if (barHeight > 0) {
                val top = paddingTop + chartHeight - barHeight
                val bottom = paddingTop + chartHeight
                val left = x - (barWidth / 2)
                val right = x + (barWidth / 2)

                val rect = RectF(left, top, right, bottom)
                
                val colorStart = ContextCompat.getColor(context, R.color.blue_primary)
                val colorEnd = ContextCompat.getColor(context, R.color.blue_light)
                
                // Đảm bảo top và bottom không bằng nhau
                barPaint.shader = LinearGradient(0f, top, 0f, bottom, colorStart, colorEnd, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, 8f * resources.displayMetrics.density, 8f * resources.displayMetrics.density, barPaint)
            }
        }
    }
}
