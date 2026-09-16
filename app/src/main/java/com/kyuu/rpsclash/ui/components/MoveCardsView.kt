package com.kyuu.rpsclash.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
            alpha = if (value) 1.0f else 0.4f
        }

    private var selectedMove: Int = NativeBridge.MOVE_NONE
    private val cardViews = mutableListOf<View>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        weightSum = 3f

        addView(createCard(NativeBridge.MOVE_ROCK, "Batu", R.drawable.ic_rock, "#FF5252"))
        addView(createCard(NativeBridge.MOVE_PAPER, "Kertas", R.drawable.ic_paper, "#448AFF"))
        addView(createCard(NativeBridge.MOVE_SCISSORS, "Gunting", R.drawable.ic_scissors, "#69F0AE"))
    }

    private fun createCard(move: Int, label: String, iconRes: Int, accentHex: String): View {
        val container = LinearLayout(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(12, 0, 12, 0)
            }
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(16, 24, 16, 24)
            isClickable = true
            isFocusable = true

            val icon = ImageView(context).apply {
                layoutParams = LayoutParams(72, 72).apply {
                    bottomMargin = 12
                }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val text = TextView(context).apply {
                this.text = label
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(text)

            setOnClickListener {
                if (!isEnabledSelection) return@setOnClickListener

                // Trigger Haptic & Sound
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MOVE_SELECTION)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_SELECT)

                // Spring micro-animation
                animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
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
        cardViews.forEachIndexed { index, view ->
            val isSelected = (index == move)
            view.alpha = if (isSelected) 1.0f else 0.6f
            view.scaleX = if (isSelected) 1.05f else 1.0f
            view.scaleY = if (isSelected) 1.05f else 1.0f
        }
    }

    fun resetSelection() {
        selectedMove = NativeBridge.MOVE_NONE
        cardViews.forEach { view ->
            view.alpha = 1.0f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
        }
    }
}
