package com.kyuu.rpsclash.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R
import com.kyuu.rpsclash.RPSApplication
import com.kyuu.rpsclash.nativebridge.NativeBridge

class MoveCardsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onMoveSelectedListener: ((Int) -> Unit)? = null
    var isEnabledSelection: Boolean = true
        set(value) {
            field = value
            animate().alpha(if (value) 1.0f else 0.45f).setDuration(150).start()
        }

    private var selectedMove: Int = NativeBridge.MOVE_NONE
    private val cardViews = mutableListOf<LinearLayout>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        weightSum = 3f

        addView(createCard(NativeBridge.MOVE_ROCK, "Batu", R.drawable.ic_rock, "#F43F5E"))
        addView(createCard(NativeBridge.MOVE_PAPER, "Kertas", R.drawable.ic_paper, "#38BDF8"))
        addView(createCard(NativeBridge.MOVE_SCISSORS, "Gunting", R.drawable.ic_scissors, "#34D399"))
    }

    private fun createCard(move: Int, label: String, iconRes: Int, accentHex: String): View {
        val container = LinearLayout(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(10, 0, 10, 0)
            }
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 20, 16, 20)
            isClickable = true
            isFocusable = true

            // Styled Card Background
            val normalBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#162032"))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#23334D"))
            }
            background = normalBg

            val icon = ImageView(context).apply {
                layoutParams = LayoutParams(64, 64).apply {
                    bottomMargin = 10
                }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val text = TextView(context).apply {
                this.text = label
                textSize = 13f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(text)

            setOnClickListener {
                if (!isEnabledSelection) return@setOnClickListener

                // Haptic & Sound
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MOVE_SELECTION)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_SELECT)

                // Spring micro-animation
                animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(120)
                        .setInterpolator(OvershootInterpolator(1.3f))
                        .start()
                }.start()

                selectMove(move)
                onMoveSelectedListener?.invoke(move)
            }
        }
        cardViews.add(container)
        return container
    }

    fun selectMove(move: Int) {
        selectedMove = move
        val accentColors = listOf("#F43F5E", "#38BDF8", "#34D399")

        cardViews.forEachIndexed { index, view ->
            val isSelected = (index == move)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (isSelected) Color.parseColor("#1E2C44") else Color.parseColor("#162032"))
                cornerRadius = 16f
                setStroke(
                    if (isSelected) 3 else 2,
                    if (isSelected) Color.parseColor(accentColors[index]) else Color.parseColor("#23334D")
                )
            }
            view.background = bg
            view.alpha = if (isSelected) 1.0f else 0.55f
            view.scaleX = if (isSelected) 1.04f else 1.0f
            view.scaleY = if (isSelected) 1.04f else 1.0f
        }
    }

    fun resetSelection() {
        selectedMove = NativeBridge.MOVE_NONE
        cardViews.forEach { view ->
            val normalBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#162032"))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#23334D"))
            }
            view.background = normalBg
            view.alpha = 1.0f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
        }
    }
}
