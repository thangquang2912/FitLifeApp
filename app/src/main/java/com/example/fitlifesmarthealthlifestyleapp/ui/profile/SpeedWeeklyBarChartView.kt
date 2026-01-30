package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.fitlifesmarthealthlifestyleapp.R

class SpeedBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<Double> = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var labels: List<String> = listOf("0-5", "5-10", "10-15", "15-20", "20-25", "25-30", "30+")
    private var maxSpeed = 15.0f

    private var animProgress = 0f

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gray_text)
        textSize = 10f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    fun setLabels(newLabels: List<String>) {
        this.labels = newLabels
        invalidate()
    }

    fun setSpeedData(speeds: List<Double>) {
        this.data = speeds
        this.maxSpeed = (speeds.maxOrNull() ?: 0.0).toFloat() * 1.2f // Thêm 20% padding
        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
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

        val paddingBottom = 30f * resources.displayMetrics.density
        val paddingTop = 10f * resources.displayMetrics.density
        val paddingLeft = 10f * resources.displayMetrics.density
        val paddingRight = 10f * resources.displayMetrics.density

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val barWidth = (chartWidth / data.size) * 0.6f
        val spacing = (chartWidth / data.size)

        data.forEachIndexed { index, speed ->
            val x = paddingLeft + (index * spacing) + (spacing / 2)

            if (index < labels.size) {
                canvas.drawText(labels[index], x, height - 10f * resources.displayMetrics.density, textPaint)
            }

            val ratio = (speed.toFloat() / maxSpeed).coerceAtMost(1f)
            val barHeight = chartHeight * ratio * animProgress

            if (barHeight > 0) {
                val top = paddingTop + chartHeight - barHeight
                val bottom = paddingTop + chartHeight
                val left = x - (barWidth / 2)
                val right = x + (barWidth / 2)

                val rect = RectF(left, top, right, bottom)

                // Màu gradient theo tốc độ
                val colorStart = ContextCompat.getColor(context, R.color.chart_bar_primary)
                val colorEnd = ContextCompat.getColor(context, R.color.chart_bar_secondary)

                barPaint.shader = LinearGradient(0f, top, 0f, bottom, colorStart, colorEnd, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, 4f * resources.displayMetrics.density, 4f * resources.displayMetrics.density, barPaint)

                // Hiển thị giá trị tốc độ
                if (barHeight > 20f * resources.displayMetrics.density) {
                    canvas.drawText(
                        String.format("%.1f", speed),
                        x,
                        top - 4f * resources.displayMetrics.density,
                        valuePaint
                    )
                }
            }
        }
    }
}