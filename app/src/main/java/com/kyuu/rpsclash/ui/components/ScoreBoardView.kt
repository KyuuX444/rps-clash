package com.kyuu.rpsclash.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R

class ScoreBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val playerDotsLayout: LinearLayout
    private val opponentDotsLayout: LinearLayout
    private val scoreText: TextView
    private val roundBadge: TextView
    private val playerNameText: TextView
    private val opponentNameText: TextView

    private var targetWins: Int = 2

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_card_glass)
        setPadding(32, 20, 32, 20)

        // Round Badge
        roundBadge = TextView(context).apply {
            text = "RONDE 1"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
            setBackgroundResource(R.drawable.bg_pill)
            setPadding(24, 6, 24, 6)
            gravity = Gravity.CENTER
        }
        addView(roundBadge)

        // Row containing Player, Score, Opponent
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16
            }
        }

        // Left: Player
        val playerCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        playerNameText = TextView(context).apply {
            text = "PLAYER"
            textSize = 14f
            setTextColor(Color.WHITE)
            isFakeBoldText = true
        }
        playerDotsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
            }
        }
        playerCol.addView(playerNameText)
        playerCol.addView(playerDotsLayout)
        row.addView(playerCol)

        // Center: Score
        scoreText = TextView(context).apply {
            text = "0 - 0"
            textSize = 28f
            setTextColor(Color.WHITE)
            isFakeBoldText = true
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(24, 0, 24, 0)
            }
        }
        row.addView(scoreText)

        // Right: Opponent
        val opponentCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        opponentNameText = TextView(context).apply {
            text = "OPPONENT"
            textSize = 14f
            setTextColor(Color.WHITE)
            isFakeBoldText = true
        }
        opponentDotsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8
            }
        }
        opponentCol.addView(opponentNameText)
        opponentCol.addView(opponentDotsLayout)
        row.addView(opponentCol)

        addView(row)

        setupTargetDots(2)
    }

    fun setNames(player: String, opponent: String) {
        playerNameText.text = player
        opponentNameText.text = opponent
    }

    fun setupTargetDots(target: Int) {
        targetWins = target
        updateDots(playerDotsLayout, 0, ContextCompat.getColor(context, R.color.neon_cyan))
        updateDots(opponentDotsLayout, 0, ContextCompat.getColor(context, R.color.neon_magenta))
    }

    fun updateScore(playerScore: Int, opponentScore: Int, round: Int) {
        scoreText.text = "$playerScore - $opponentScore"
        roundBadge.text = "RONDE $round"
        updateDots(playerDotsLayout, playerScore, ContextCompat.getColor(context, R.color.neon_cyan))
        updateDots(opponentDotsLayout, opponentScore, ContextCompat.getColor(context, R.color.neon_magenta))
    }

    private fun updateDots(container: LinearLayout, score: Int, activeColor: Int) {
        container.removeAllViews()
        for (i in 0 until targetWins) {
            val dot = View(context).apply {
                val size = 18
                layoutParams = LayoutParams(size, size).apply {
                    setMargins(4, 0, 4, 0)
                }
                setBackgroundColor(if (i < score) activeColor else Color.parseColor("#334155"))
            }
            container.addView(dot)
        }
    }
}
