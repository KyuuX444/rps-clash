package com.kyuu.rpsclash.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R
import com.kyuu.rpsclash.nativebridge.NativeBridge

class ArenaHandsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rockDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_rock)
    private val paperDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_paper)
    private val scissorsDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_scissors)

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val shockwavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1A2234")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    var playerMove: Int = NativeBridge.MOVE_NONE
    var opponentMove: Int = NativeBridge.MOVE_NONE
    var roundResult: Int = NativeBridge.RESULT_NONE

    private var clashProgress: Float = 0f
    private var shockwaveRadius: Float = 0f
    private var isClashing: Boolean = false

    fun startClashAnimation(pMove: Int, oMove: Int, result: Int, onComplete: () -> Unit) {
        playerMove = pMove
        opponentMove = oMove
        roundResult = result
        isClashing = true
        clashProgress = 0f
        shockwaveRadius = 0f

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                clashProgress = it.animatedFraction
                shockwaveRadius = clashProgress * width * 0.4f
                invalidate()
            }
        }
        animator.start()
        postDelayed({
            isClashing = false
            onComplete()
        }, 700)
    }

    fun reset() {
        playerMove = NativeBridge.MOVE_NONE
        opponentMove = NativeBridge.MOVE_NONE
        roundResult = NativeBridge.RESULT_NONE
        isClashing = false
        clashProgress = 0f
        shockwaveRadius = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val cardSize = width * 0.32f

        // Draw Shockwave if clashing
        if (isClashing && clashProgress > 0.4f) {
            val alpha = ((1f - (clashProgress - 0.4f) / 0.6f) * 255).toInt().coerceIn(0, 255)
            val shockColor = when (roundResult) {
                NativeBridge.RESULT_WIN -> Color.parseColor("#00E676")
                NativeBridge.RESULT_LOSE -> Color.parseColor("#FF1744")
                else -> Color.parseColor("#FF9100")
            }
            shockwavePaint.color = shockColor
            shockwavePaint.alpha = alpha
            shockwavePaint.strokeWidth = 8f * (1f - clashProgress)
            canvas.drawCircle(cx, cy, shockwaveRadius, shockwavePaint)
        }

        // Opponent Hand (Top)
        val oppY = if (isClashing) {
            val startY = cy - cardSize * 1.2f
            startY + (cy - startY) * clashProgress * 0.7f
        } else {
            cy - cardSize * 1.1f
        }
        drawHandCard(canvas, cx, oppY, cardSize, opponentMove, isPlayer = false)

        // Player Hand (Bottom)
        val playY = if (isClashing) {
            val startY = cy + cardSize * 1.2f
            startY - (startY - cy) * clashProgress * 0.7f
        } else {
            cy + cardSize * 1.1f
        }
        drawHandCard(canvas, cx, playY, cardSize, playerMove, isPlayer = true)
    }

    private fun drawHandCard(canvas: Canvas, x: Float, y: Float, size: Float, move: Int, isPlayer: Boolean) {
        val rect = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)

        // Card Background
        canvas.drawRoundRect(rect, 24f, 24f, cardPaint)

        // Glow Border
        val borderColor = when {
            move == NativeBridge.MOVE_NONE -> Color.parseColor("#334155")
            !isClashing -> Color.parseColor("#38BDF8")
            isPlayer && roundResult == NativeBridge.RESULT_WIN -> Color.parseColor("#00E676")
            !isPlayer && roundResult == NativeBridge.RESULT_LOSE -> Color.parseColor("#00E676")
            isPlayer && roundResult == NativeBridge.RESULT_LOSE -> Color.parseColor("#FF1744")
            !isPlayer && roundResult == NativeBridge.RESULT_WIN -> Color.parseColor("#FF1744")
            else -> Color.parseColor("#FF9100")
        }
        glowPaint.color = borderColor
        canvas.drawRoundRect(rect, 24f, 24f, glowPaint)

        // Draw Icon
        val drawable = when (move) {
            NativeBridge.MOVE_ROCK -> rockDrawable
            NativeBridge.MOVE_PAPER -> paperDrawable
            NativeBridge.MOVE_SCISSORS -> scissorsDrawable
            else -> null
        }

        if (drawable != null) {
            val iconInset = size * 0.2f
            drawable.setBounds(
                (rect.left + iconInset).toInt(),
                (rect.top + iconInset).toInt(),
                (rect.right - iconInset).toInt(),
                (rect.bottom - iconInset).toInt()
            )
            drawable.draw(canvas)
        } else {
            // Placeholder text (e.g. "?" or "Choosing")
            textPaint.color = Color.parseColor("#64748B")
            textPaint.textSize = size * 0.35f
            canvas.drawText("?", x, y + size * 0.12f, textPaint)
        }
    }
}
