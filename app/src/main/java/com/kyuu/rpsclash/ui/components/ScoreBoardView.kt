package com.kyuu.rpsclash.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.R

/**
 * ScoreBoardView implements the top section of the focused Game Screen:
 * Opponent Info (Avatar, Name, Win Dots)
 *   ↓
 * Big Score (32sp, clean display)
 *   ↓
 * Round / Countdown Status Pill
 *
 * It also coordinates the synchronized PlayerStatusView (Player Avatar, Name, Win Dots)
 * positioned right below the Game Arena.
 */
class ScoreBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val opponentNameText: TextView
    private val opponentDotsLayout: LinearLayout
    private val scoreText: TextView
    private val roundPill: TextView

    // Synchronized Player View
    private var playerNameText: TextView? = null
    private var playerDotsLayout: LinearLayout? = null
    private var playerStatusViewInstance: View? = null

    private var targetWins: Int = 2
    private var currentOpponentScore: Int = 0
    private var currentPlayerScore: Int = 0

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // 1. OPPONENT SECTION (Avatar, Name, Win Dots)
        val opponentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            val oppIcon = ImageView(context).apply {
                layoutParams = LayoutParams(30, 30).apply { marginEnd = 8 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_robot))
            }
            addView(oppIcon)

            opponentNameText = TextView(context).apply {
                text = "AI BOT"
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.05f
            }
            addView(opponentNameText)

            opponentDotsLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 12
                }
            }
            addView(opponentDotsLayout)
        }
        addView(opponentRow)

        // 2. SCORE SECTION (Big Clean Typography 32sp)
        scoreText = TextView(context).apply {
            text = "0 - 0"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
                bottomMargin = 6
            }
        }
        addView(scoreText)

        // 3. ROUND / COUNTDOWN PILL
        roundPill = TextView(context).apply {
            text = "RONDE 1"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_pill)
            setPadding(20, 6, 20, 6)
            gravity = Gravity.CENTER
        }
        addView(roundPill)

        setupTargetDots(2)
    }

    /**
     * Creates and returns the Player Status View to be placed below the Game Arena
     */
    fun createPlayerStatusView(): View {
        if (playerStatusViewInstance != null) {
            return playerStatusViewInstance!!
        }

        val playerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
                bottomMargin = 10
            }

            val pIcon = ImageView(context).apply {
                layoutParams = LayoutParams(30, 30).apply { marginEnd = 8 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_user))
            }
            addView(pIcon)

            playerNameText = TextView(context).apply {
                text = "PLAYER"
                textSize = 13f
                setTextColor(Color.parseColor("#3B82F6"))
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.05f
            }
            addView(playerNameText)

            playerDotsLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 12
                }
            }
            addView(playerDotsLayout)
        }

        playerStatusViewInstance = playerRow
        updateDots(playerDotsLayout!!, currentPlayerScore, Color.parseColor("#3B82F6"))
        return playerRow
    }

    fun setNames(player: String, opponent: String) {
        playerNameText?.text = player
        opponentNameText.text = opponent
    }

    fun setupTargetDots(target: Int) {
        targetWins = target
        updateDots(opponentDotsLayout, currentOpponentScore, Color.parseColor("#64748B"))
        playerDotsLayout?.let {
            updateDots(it, currentPlayerScore, Color.parseColor("#3B82F6"))
        }
    }

    fun updateScore(playerScore: Int, opponentScore: Int, round: Int) {
        currentPlayerScore = playerScore
        currentOpponentScore = opponentScore

        scoreText.text = "$playerScore - $opponentScore"
        roundPill.text = "RONDE $round"
        roundPill.setTextColor(ContextCompat.getColor(context, R.color.accent_primary))

        updateDots(opponentDotsLayout, opponentScore, Color.parseColor("#64748B"))
        playerDotsLayout?.let {
            updateDots(it, playerScore, Color.parseColor("#3B82F6"))
        }
    }

    fun setRoundStatus(status: String, colorHex: String? = null) {
        roundPill.text = status
        if (colorHex != null) {
            roundPill.setTextColor(Color.parseColor(colorHex))
        }
    }

    private fun updateDots(container: LinearLayout, score: Int, activeColor: Int) {
        container.removeAllViews()
        for (i in 0 until targetWins) {
            val dot = View(context).apply {
                val size = 16
                val margin = 5
                layoutParams = LayoutParams(size, size).apply {
                    setMargins(margin, 0, margin, 0)
                }
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (i < score) {
                        setColor(activeColor)
                    } else {
                        setColor(Color.parseColor("#1E293B"))
                        setStroke(1, Color.parseColor("#334155"))
                    }
                }
                background = bg
            }
            container.addView(dot)
        }
    }
}
