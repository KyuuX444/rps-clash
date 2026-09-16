package com.kyuu.rpsclash.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.kyuu.rpsclash.data.ModeStatsData

class MoveDistributionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F43F5E") }
    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#38BDF8") }
    private val scissorsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399") }

    private var rockRatio: Float = 0.333f
    private var paperRatio: Float = 0.333f
    private var scissorsRatio: Float = 0.334f

    fun setStats(stats: ModeStatsData) {
        val total = stats.totalMoves
        if (total > 0) {
            rockRatio = stats.rockUsage.toFloat() / total
            paperRatio = stats.paperUsage.toFloat() / total
            scissorsRatio = stats.scissorsUsage.toFloat() / total
        } else {
            rockRatio = 0.333f
            paperRatio = 0.333f
            scissorsRatio = 0.334f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f

        val rockW = w * rockRatio
        val paperW = w * paperRatio
        val scissorsW = w * scissorsRatio

        // Draw segmented rounded bar
        canvas.save()
        val bounds = RectF(0f, 0f, w, h)
        // Rock segment
        canvas.drawRoundRect(bounds, r, r, rockPaint)

        // Paper segment
        val paperRect = RectF(rockW, 0f, rockW + paperW, h)
        canvas.drawRect(paperRect, paperPaint)

        // Scissors segment
        val scissorsRect = RectF(rockW + paperW, 0f, w, h)
        canvas.drawRect(scissorsRect, scissorsPaint)

        canvas.restore()
    }
}

class WinRateGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#1E2C44")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#00E5FF")
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 20f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.1f
    }

    private var winRate: Float = 0.0f

    fun setWinRate(rate: Float) {
        winRate = rate.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = (size - 32f) / 2f

        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Background Arc
        canvas.drawArc(oval, 135f, 270f, false, bgPaint)

        // Progress Arc
        val sweep = (winRate / 100f) * 270f
        canvas.drawArc(oval, 135f, sweep, false, progressPaint)

        // Text
        val rateText = String.format("%.1f%%", winRate)
        canvas.drawText(rateText, cx, cy + 10f, textPaint)
        canvas.drawText("WIN RATE", cx, cy + 38f, labelPaint)
    }
}
