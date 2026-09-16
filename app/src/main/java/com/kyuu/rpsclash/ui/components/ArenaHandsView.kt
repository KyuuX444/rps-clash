package com.kyuu.rpsclash.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
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

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#334155")
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val shockwavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
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
            duration = 550
            interpolator = OvershootInterpolator(1.08f)
            addUpdateListener {
                clashProgress = it.animatedFraction
                shockwaveRadius = clashProgress * width * 0.35f
                invalidate()
            }
        }
        animator.start()
        postDelayed({
            isClashing = false
            onComplete()
        }, 600)
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
        val cardSize = (width * 0.28f).coerceIn(88f, 130f)

        // Center Arena Battle Ring
        canvas.drawCircle(cx, cy, cardSize * 1.15f, ringPaint)

        // Subtle shockwave effect on collision
        if (isClashing && clashProgress > 0.35f) {
            val alpha = ((1f - (clashProgress - 0.35f) / 0.65f) * 220).toInt().coerceIn(0, 255)
            val shockColor = when (roundResult) {
                NativeBridge.RESULT_WIN -> Color.parseColor("#10B981")
                NativeBridge.RESULT_LOSE -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#F59E0B")
            }
            shockwavePaint.color = shockColor
            shockwavePaint.alpha = alpha
            shockwavePaint.strokeWidth = 4f * (1f - clashProgress)
            canvas.drawCircle(cx, cy, shockwaveRadius, shockwavePaint)
        }

        // Opponent Hand (Top)
        val oppY = if (isClashing) {
            val startY = cy - cardSize * 1.05f
            startY + (cy - startY) * clashProgress * 0.65f
        } else {
            cy - cardSize * 0.95f
        }
        drawHandCard(canvas, cx, oppY, cardSize, opponentMove, isPlayer = false)

        // Player Hand (Bottom)
        val playY = if (isClashing) {
            val startY = cy + cardSize * 1.05f
            startY - (startY - cy) * clashProgress * 0.65f
        } else {
            cy + cardSize * 0.95f
        }
        drawHandCard(canvas, cx, playY, cardSize, playerMove, isPlayer = true)
    }

    private fun drawHandCard(canvas: Canvas, x: Float, y: Float, size: Float, move: Int, isPlayer: Boolean) {
        val rect = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)

        // Background
        canvas.drawRoundRect(rect, 16f, 16f, cardPaint)

        // Non-neon clean border
        val borderColor = when {
            move == NativeBridge.MOVE_NONE -> Color.parseColor("#334155")
            !isClashing -> Color.parseColor("#3B82F6")
            isPlayer && roundResult == NativeBridge.RESULT_WIN -> Color.parseColor("#10B981")
            !isPlayer && roundResult == NativeBridge.RESULT_LOSE -> Color.parseColor("#10B981")
            isPlayer && roundResult == NativeBridge.RESULT_LOSE -> Color.parseColor("#EF4444")
            !isPlayer && roundResult == NativeBridge.RESULT_WIN -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#F59E0B")
        }
        borderPaint.color = borderColor
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

        // Draw Icon
        val drawable = when (move) {
            NativeBridge.MOVE_ROCK -> rockDrawable
            NativeBridge.MOVE_PAPER -> paperDrawable
            NativeBridge.MOVE_SCISSORS -> scissorsDrawable
            else -> null
        }

        if (drawable != null) {
            val iconInset = size * 0.22f
            drawable.setBounds(
                (rect.left + iconInset).toInt(),
                (rect.top + iconInset).toInt(),
                (rect.right - iconInset).toInt(),
                (rect.bottom - iconInset).toInt()
            )
            drawable.draw(canvas)
        } else {
            textPaint.textSize = size * 0.32f
            canvas.drawText("?", x, y + size * 0.11f, textPaint)
        }
    }
}
