package com.kyuu.rpsclash

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kyuu.rpsclash.data.GameStatsData
import com.kyuu.rpsclash.data.ModeStatsData
import com.kyuu.rpsclash.nativebridge.NativeBridge
import com.kyuu.rpsclash.network.ConnectionState
import com.kyuu.rpsclash.network.OnlineGameListener
import com.kyuu.rpsclash.network.WebSocketManager
import com.kyuu.rpsclash.ui.ScreenState
import com.kyuu.rpsclash.ui.components.*
import com.kyuu.rpsclash.ui.components.isFakeBoldText

class MainActivity : AppCompatActivity(), OnlineGameListener {

    private lateinit var rootContainer: FrameLayout
    private var currentScreen: ScreenState = ScreenState.SPLASH

    // Online & Game state
    private val wsManager = WebSocketManager()
    private var vsAiBestOf = 3
    private var vsAiDifficulty = NativeBridge.DIFF_NORMAL
    private var selectedMoveThisRound = NativeBridge.MOVE_NONE
    private var roundTimer: CountDownTimer? = null

    // Tracking match moves for stats
    private var matchRockCount = 0
    private var matchPaperCount = 0
    private var matchScissorsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_main))
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(rootContainer)

        wsManager.listener = this

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentScreen) {
                    ScreenState.SPLASH -> finish()
                    ScreenState.HOME -> finish()
                    ScreenState.VS_AI_MATCH, ScreenState.ONLINE_MATCH -> {
                        CustomDialogs.showErrorDialog(
                            this@MainActivity,
                            "Keluar Pertandingan",
                            "Apakah Anda yakin ingin meninggalkan pertandingan ini?"
                        ) {
                            if (currentScreen == ScreenState.ONLINE_MATCH) {
                                wsManager.sendLeaveMatch()
                            }
                            navigateTo(ScreenState.HOME)
                        }
                    }
                    ScreenState.ONLINE_WAITING -> {
                        wsManager.sendLeaveMatch()
                        navigateTo(ScreenState.ONLINE_LOBBY)
                    }
                    else -> navigateTo(ScreenState.HOME)
                }
            }
        })

        // Start BGM
        RPSApplication.instance.soundManager.startBgm()

        // Show Splash initially
        showSplashScreen()
    }

    override fun onResume() {
        super.onResume()
        RPSApplication.instance.soundManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        RPSApplication.instance.soundManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnect()
        roundTimer?.cancel()
    }

    private fun navigateTo(screen: ScreenState) {
        currentScreen = screen
        rootContainer.removeAllViews()
        roundTimer?.cancel()

        when (screen) {
            ScreenState.SPLASH -> showSplashScreen()
            ScreenState.HOME -> showHomeScreen()
            ScreenState.VS_AI_SETUP -> showVsAiSetupScreen()
            ScreenState.VS_AI_MATCH -> showVsAiMatchScreen()
            ScreenState.ONLINE_LOBBY -> showOnlineLobbyScreen()
            ScreenState.ONLINE_WAITING -> showOnlineWaitingScreen()
            ScreenState.ONLINE_MATCH -> showOnlineMatchScreen()
            ScreenState.STATS -> showStatsScreen()
            ScreenState.HOW_TO_PLAY -> showHowToPlayScreen()
            ScreenState.FAQ -> showFaqScreen()
            ScreenState.ABOUT -> showAboutScreen()
            ScreenState.SETTINGS -> showSettingsScreen()
        }
    }

    // =========================================================================
    // 1. SPLASH SCREEN
    // =========================================================================
    private fun showSplashScreen() {
        val splashLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(48, 48, 48, 48)

            val logo = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(160, 160).apply { bottomMargin = 24 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
            }
            addView(logo)

            // Pulse animation for logo
            val animator = ValueAnimator.ofFloat(0.92f, 1.08f).apply {
                duration = 800
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val scale = it.animatedValue as Float
                    logo.scaleX = scale
                    logo.scaleY = scale
                }
            }
            animator.start()

            val title = TextView(context).apply {
                text = "RPS CLASH"
                textSize = 34f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                letterSpacing = 0.1f
            }
            addView(title)

            val subtitle = TextView(context).apply {
                text = getString(R.string.app_subtitle)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 8
                    bottomMargin = 48
                }
            }
            addView(subtitle)

            val progressBar = ProgressBar(context).apply {
                indeterminateTintList = ContextCompat.getColorStateList(context, R.color.neon_cyan)
            }
            addView(progressBar)

            val version = TextView(context).apply {
                text = "v1.0.0 • Native C++ Core Engine"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 40
                }
            }
            addView(version)

            setOnClickListener {
                animator.cancel()
                navigateTo(ScreenState.HOME)
            }
        }
        rootContainer.addView(splashLayout)

        Handler(Looper.getMainLooper()).postDelayed({
            if (currentScreen == ScreenState.SPLASH) {
                navigateTo(ScreenState.HOME)
            }
        }, 1800)
    }

    // =========================================================================
    // 2. HOME SCREEN
    // =========================================================================
    private fun showHomeScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val homeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 48, 36, 48)

            // Header Logo & Title
            val logo = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply { bottomMargin = 12 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
            }
            addView(logo)

            val title = TextView(context).apply {
                text = "RPS CLASH"
                textSize = 28f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
            }
            addView(title)

            val tag = TextView(context).apply {
                text = "BATU • GUNTING • KERTAS"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    bottomMargin = 32
                }
            }
            addView(tag)

            // Main Play Modes
            addView(createBigActionCard(
                title = "VS AI (OFFLINE)",
                desc = "Lawan AI taktis dengan Markov Chain engine",
                badge = "OFFLINE",
                badgeColor = "#00E676",
                accentHex = "#00E5FF"
            ) {
                navigateTo(ScreenState.VS_AI_SETUP)
            })

            addView(createBigActionCard(
                title = "ONLINE MULTIPLAYER",
                desc = "Duel PvP real-time via WebSocket room",
                badge = "REAL-TIME",
                badgeColor = "#D500F9",
                accentHex = "#D500F9"
            ) {
                navigateTo(ScreenState.ONLINE_LOBBY)
            })

            // Secondary Menu Grid
            val menuGrid = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 16
                }
            }

            val row1 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 12 }
                addView(createSmallActionCard("STATISTIK", R.drawable.ic_trophy) { navigateTo(ScreenState.STATS) })
                addView(createSmallActionCard("CARA BERMAIN", R.drawable.ic_vs) { navigateTo(ScreenState.HOW_TO_PLAY) })
            }
            menuGrid.addView(row1)

            val row2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 12 }
                addView(createSmallActionCard("FAQ", R.drawable.ic_chevron_down) { navigateTo(ScreenState.FAQ) })
                addView(createSmallActionCard("DEVELOPER", R.drawable.ic_github) { navigateTo(ScreenState.ABOUT) })
            }
            menuGrid.addView(row2)

            val row3 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 1f
                addView(createSmallActionCard("PENGATURAN", R.drawable.ic_rock) { navigateTo(ScreenState.SETTINGS) })
            }
            menuGrid.addView(row3)

            addView(menuGrid)
        }

        scroll.addView(homeLayout)
        rootContainer.addView(scroll)
    }

    private fun createBigActionCard(
        title: String,
        desc: String,
        badge: String,
        badgeColor: String,
        accentHex: String,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            setPadding(32, 28, 32, 28)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 20
            }

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val titleView = TextView(context).apply {
                    text = title
                    textSize = 18f
                    setTextColor(Color.parseColor(accentHex))
                    isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }
                addView(titleView)

                val badgeView = TextView(context).apply {
                    text = badge
                    textSize = 10f
                    setTextColor(Color.parseColor(badgeColor))
                    setBackgroundResource(R.drawable.bg_pill)
                    setPadding(16, 6, 16, 6)
                }
                addView(badgeView)
            }
            addView(topRow)

            val descView = TextView(context).apply {
                text = desc
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 10
                }
            }
            addView(descView)

            setOnClickListener {
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_BUTTON_PRESS)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                onClick()
            }
        }
    }

    private fun createSmallActionCard(title: String, iconRes: Int, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 8, 0)
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40).apply { bottomMargin = 8 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val titleView = TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(titleView)

            setOnClickListener {
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_BUTTON_PRESS)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                onClick()
            }
        }
    }

    // =========================================================================
    // 3. VS AI SETUP SCREEN
    // =========================================================================
    private fun showVsAiSetupScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            // Top Bar
            addView(createTopBar("PENGATURAN MATCH AI") { navigateTo(ScreenState.HOME) })

            // Difficulty Section
            val diffTitle = TextView(context).apply {
                text = getString(R.string.title_choose_difficulty)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 32
                    bottomMargin = 16
                }
            }
            addView(diffTitle)

            var diffEasyCard: View? = null
            var diffNormalCard: View? = null
            var diffHardCard: View? = null

            fun updateDiffCards(selected: Int) {
                vsAiDifficulty = selected
                diffEasyCard?.alpha = if (selected == NativeBridge.DIFF_EASY) 1.0f else 0.5f
                diffNormalCard?.alpha = if (selected == NativeBridge.DIFF_NORMAL) 1.0f else 0.5f
                diffHardCard?.alpha = if (selected == NativeBridge.DIFF_HARD) 1.0f else 0.5f
            }

            diffEasyCard = createSelectableCard("MUDAH", getString(R.string.diff_easy_desc), "#00E676") {
                updateDiffCards(NativeBridge.DIFF_EASY)
            }
            diffNormalCard = createSelectableCard("NORMAL", getString(R.string.diff_normal_desc), "#00E5FF") {
                updateDiffCards(NativeBridge.DIFF_NORMAL)
            }
            diffHardCard = createSelectableCard("HARD (MARKOV CHAIN)", getString(R.string.diff_hard_desc), "#D500F9") {
                updateDiffCards(NativeBridge.DIFF_HARD)
            }

            addView(diffEasyCard)
            addView(diffNormalCard)
            addView(diffHardCard)
            updateDiffCards(vsAiDifficulty)

            // Format Section
            val formatTitle = TextView(context).apply {
                text = getString(R.string.title_match_format)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 28
                    bottomMargin = 16
                }
            }
            addView(formatTitle)

            var bo3Card: View? = null
            var bo5Card: View? = null

            fun updateFormatCards(bo: Int) {
                vsAiBestOf = bo
                bo3Card?.alpha = if (bo == 3) 1.0f else 0.5f
                bo5Card?.alpha = if (bo == 5) 1.0f else 0.5f
            }

            bo3Card = createSelectableCard("BEST OF 3", "Pemenang pertama yang mencapai 2 poin", "#FFD600") {
                updateFormatCards(3)
            }
            bo5Card = createSelectableCard("BEST OF 5", "Pemenang pertama yang mencapai 3 poin", "#FF9100") {
                updateFormatCards(5)
            }
            addView(bo3Card)
            addView(bo5Card)
            updateFormatCards(vsAiBestOf)

            // Start Match Button
            val btnStart = Button(context).apply {
                text = getString(R.string.btn_start_match)
                setTextColor(Color.BLACK)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 40
                }
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_START)
                    navigateTo(ScreenState.VS_AI_MATCH)
                }
            }
            addView(btnStart)
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createSelectableCard(title: String, desc: String, accentHex: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(28, 20, 28, 20)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 12
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(Color.parseColor(accentHex))
                isFakeBoldText = true
            }
            addView(titleView)

            val descView = TextView(context).apply {
                text = desc
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 4
                }
            }
            addView(descView)

            setOnClickListener {
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_BUTTON_PRESS)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                onClick()
            }
        }
    }

    // =========================================================================
    // 4. VS AI MATCH SCREEN
    // =========================================================================
    private fun showVsAiMatchScreen() {
        // Reset match stats in C++
        NativeBridge.startNewVsAiMatch(vsAiBestOf)
        matchRockCount = 0
        matchPaperCount = 0
        matchScissorsCount = 0

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(32, 28, 32, 28)
        }

        // Top Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val btnExit = ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    CustomDialogs.showErrorDialog(this@MainActivity, "Keluar", "Keluar dari pertandingan?") {
                        navigateTo(ScreenState.HOME)
                    }
                }
            }
            addView(btnExit)

            val diffBadge = TextView(context).apply {
                val diffStr = when (vsAiDifficulty) {
                    NativeBridge.DIFF_EASY -> "AI: MUDAH"
                    NativeBridge.DIFF_NORMAL -> "AI: NORMAL"
                    else -> "AI: HARD"
                }
                text = diffStr
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(24, 6, 24, 6)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginStart = 20
                }
            }
            addView(diffBadge)
        }
        layout.addView(topBar)

        // Scoreboard
        val targetWins = if (vsAiBestOf == 5) 3 else 2
        val scoreBoard = ScoreBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 16
                bottomMargin = 16
            }
            setNames("PLAYER", "AI BOT")
            setupTargetDots(targetWins)
            updateScore(0, 0, 1)
        }
        layout.addView(scoreBoard)

        // Arena View (Clashing Hands)
        val arenaView = ArenaHandsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }
        layout.addView(arenaView)

        // Status & Countdown
        val statusText = TextView(this).apply {
            text = getString(R.string.status_choose_move)
            textSize = 15f
            setTextColor(Color.WHITE)
            isFakeBoldText = true
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
        }
        layout.addView(statusText)

        // Move Selection Cards
        val moveCards = MoveCardsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
        }
        layout.addView(moveCards)

        rootContainer.addView(layout)

        // Round resolution logic
        fun startVsAiRound(playerMove: Int) {
            moveCards.isEnabledSelection = false
            statusText.text = getString(R.string.status_revealing)

            when (playerMove) {
                NativeBridge.MOVE_ROCK -> matchRockCount++
                NativeBridge.MOVE_PAPER -> matchPaperCount++
                NativeBridge.MOVE_SCISSORS -> matchScissorsCount++
            }

            // Call Native C++ Engine
            val roundResultArr = NativeBridge.playVsAiRound(playerMove, vsAiDifficulty)
            val aiMove = roundResultArr[0]
            val roundRes = roundResultArr[1]
            val pScore = roundResultArr[2]
            val aiScore = roundResultArr[3]
            val isGameOver = roundResultArr[4] == 1
            val nextRound = roundResultArr[5]

            // Trigger sound & clash animation
            arenaView.startClashAnimation(playerMove, aiMove, roundRes) {
                when (roundRes) {
                    NativeBridge.RESULT_WIN -> {
                        statusText.text = getString(R.string.result_win)
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.emerald_win))
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_WIN)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_WIN)
                    }
                    NativeBridge.RESULT_LOSE -> {
                        statusText.text = getString(R.string.result_lose)
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.ruby_lose))
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_LOSE)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_LOSE)
                    }
                    else -> {
                        statusText.text = getString(R.string.result_draw)
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.amber_draw))
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_DRAW)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_DRAW)
                    }
                }

                scoreBoard.updateScore(pScore, aiScore, nextRound)

                if (isGameOver) {
                    val won = pScore > aiScore
                    val draws = (pScore + aiScore) // approx draw count
                    NativeBridge.recordMatchResult(
                        false, pScore, aiScore, 0,
                        matchRockCount, matchPaperCount, matchScissorsCount
                    )
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MATCH_RESULT)
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_END)

                    Handler(Looper.getMainLooper()).postDelayed({
                        CustomDialogs.showMatchResultDialog(
                            this,
                            won,
                            if (won) getString(R.string.match_victory) else getString(R.string.match_defeat),
                            "$pScore - $aiScore",
                            onRematch = { showVsAiMatchScreen() },
                            onExit = { navigateTo(ScreenState.HOME) }
                        )
                    }, 800)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        arenaView.reset()
                        moveCards.resetSelection()
                        moveCards.isEnabledSelection = true
                        statusText.text = getString(R.string.status_choose_move)
                        statusText.setTextColor(Color.WHITE)
                    }, 1400)
                }
            }
        }

        moveCards.onMoveSelectedListener = { move ->
            startVsAiRound(move)
        }
    }

    // =========================================================================
    // 5. ONLINE LOBBY SCREEN
    // =========================================================================
    private fun showOnlineLobbyScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            // Top Bar
            addView(createTopBar("ONLINE MULTIPLAYER") { navigateTo(ScreenState.HOME) })

            // Server Connection Status
            val serverCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(24, 16, 24, 16)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 24
                    bottomMargin = 24
                }

                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(20, 20).apply { marginEnd = 16 }
                    val color = if (wsManager.state == ConnectionState.CONNECTED) "#00E676" else "#FF9100"
                    setBackgroundColor(Color.parseColor(color))
                }
                addView(dot)

                val status = TextView(context).apply {
                    text = when (wsManager.state) {
                        ConnectionState.CONNECTED -> "Terhubung: ${RPSApplication.instance.preferences.serverUrl}"
                        ConnectionState.CONNECTING -> "Menghubungkan ke server..."
                        ConnectionState.RECONNECTING -> "Menyambung ulang..."
                        else -> "Klik untuk menghubungkan ke server"
                    }
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                addView(status)

                setOnClickListener {
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...")
                }
            }
            addView(serverCard)

            // Connect immediately if disconnected
            if (wsManager.state == ConnectionState.DISCONNECTED) {
                wsManager.connect(RPSApplication.instance.preferences.serverUrl)
            }

            // Action Cards
            addView(createBigActionCard(
                title = getString(R.string.btn_quick_match),
                desc = getString(R.string.btn_quick_match_desc),
                badge = "INSTANT",
                badgeColor = "#00E5FF",
                accentHex = "#00E5FF"
            ) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createBigActionCard
                }
                wsManager.sendQuickMatch()
                navigateTo(ScreenState.ONLINE_WAITING)
            })

            addView(createBigActionCard(
                title = getString(R.string.btn_create_room),
                desc = getString(R.string.btn_create_room_desc),
                badge = "HOST",
                badgeColor = "#D500F9",
                accentHex = "#D500F9"
            ) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createBigActionCard
                }
                wsManager.sendCreateRoom(3)
            })

            addView(createBigActionCard(
                title = getString(R.string.btn_join_room),
                desc = getString(R.string.btn_join_room_desc),
                badge = "CODE",
                badgeColor = "#FFD600",
                accentHex = "#FFD600"
            ) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createBigActionCard
                }
                CustomDialogs.showJoinRoomDialog(this@MainActivity) { code ->
                    wsManager.sendJoinRoom(code)
                }
            })
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    // =========================================================================
    // 6. ONLINE WAITING SCREEN
    // =========================================================================
    private fun showOnlineWaitingScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 48, 40, 48)
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

            addView(createTopBar("WAITING ROOM") {
                wsManager.sendLeaveMatch()
                navigateTo(ScreenState.ONLINE_LOBBY)
            })

            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
            }
            addView(spacer)

            val spinner = ProgressBar(context).apply {
                indeterminateTintList = ContextCompat.getColorStateList(context, R.color.neon_cyan)
                layoutParams = LinearLayout.LayoutParams(80, 80).apply { bottomMargin = 24 }
            }
            addView(spinner)

            val waitingMsg = TextView(context).apply {
                text = getString(R.string.waiting_for_opponent)
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(waitingMsg)

            // Room code display
            val code = wsManager.currentRoomCode
            if (code != null) {
                val codeCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_card_glass)
                    setPadding(40, 24, 40, 24)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        topMargin = 32
                        bottomMargin = 24
                    }

                    val label = TextView(context).apply {
                        text = "KODE ROOM ANDA"
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                    }
                    addView(label)

                    val codeText = TextView(context).apply {
                        text = code
                        textSize = 36f
                        setTextColor(Color.WHITE)
                        isFakeBoldText = true
                        letterSpacing = 0.2f
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            topMargin = 8
                            bottomMargin = 16
                        }
                    }
                    addView(codeText)

                    val btnCopy = Button(context).apply {
                        text = "SALIN KODE ROOM"
                        setTextColor(Color.BLACK)
                        isFakeBoldText = true
                        setBackgroundResource(R.drawable.bg_button_primary)
                        setOnClickListener {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("RPS Room Code", code))
                            CustomToast.show(this@MainActivity, getString(R.string.code_copied))
                        }
                    }
                    addView(btnCopy)
                }
                addView(codeCard)
            }

            val spacerBottom = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
            }
            addView(spacerBottom)

            val btnCancel = Button(context).apply {
                text = "BATALKAN"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_button_danger)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setOnClickListener {
                    wsManager.sendLeaveMatch()
                    navigateTo(ScreenState.ONLINE_LOBBY)
                }
            }
            addView(btnCancel)
        }

        rootContainer.addView(layout)
    }

    // =========================================================================
    // 7. ONLINE MATCH SCREEN
    // =========================================================================
    private var onlineArenaView: ArenaHandsView? = null
    private var onlineScoreBoard: ScoreBoardView? = null
    private var onlineMoveCards: MoveCardsView? = null
    private var onlineStatusText: TextView? = null

    private fun showOnlineMatchScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(32, 28, 32, 28)
        }

        // Top Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val btnExit = ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    CustomDialogs.showErrorDialog(this@MainActivity, "Keluar", "Keluar dari match online?") {
                        wsManager.sendLeaveMatch()
                        navigateTo(ScreenState.HOME)
                    }
                }
            }
            addView(btnExit)

            val roomBadge = TextView(context).apply {
                text = "ROOM: ${wsManager.currentRoomCode ?: "PVP"}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.neon_magenta))
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(24, 6, 24, 6)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    marginStart = 20
                }
            }
            addView(roomBadge)
        }
        layout.addView(topBar)

        onlineScoreBoard = ScoreBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 16
                bottomMargin = 16
            }
            setNames("PLAYER", "LAWAN")
            setupTargetDots(2)
            updateScore(0, 0, 1)
        }
        layout.addView(onlineScoreBoard)

        onlineArenaView = ArenaHandsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }
        layout.addView(onlineArenaView)

        onlineStatusText = TextView(this).apply {
            text = getString(R.string.status_choose_move)
            textSize = 15f
            setTextColor(Color.WHITE)
            isFakeBoldText = true
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
        }
        layout.addView(onlineStatusText)

        onlineMoveCards = MoveCardsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            onMoveSelectedListener = { move ->
                selectedMoveThisRound = move
                isEnabledSelection = false
                onlineStatusText?.text = "Move terkirim! Menunggu lawan..."
                wsManager.sendSubmitMove(move)
            }
        }
        layout.addView(onlineMoveCards)

        rootContainer.addView(layout)
    }

    // =========================================================================
    // 8. STATS SCREEN
    // =========================================================================
    private fun showStatsScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            addView(createTopBar(getString(R.string.title_stats)) { navigateTo(ScreenState.HOME) })

            // Tab Selector
            var isOnlineTab = false
            val tabContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card_surface)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 24
                    bottomMargin = 24
                }
            }

            val tabAi = Button(context).apply {
                text = "VS AI"
                setTextColor(Color.BLACK)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }

            val tabOnline = Button(context).apply {
                text = "ONLINE"
                setTextColor(Color.parseColor("#94A3B8"))
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }

            tabContainer.addView(tabAi)
            tabContainer.addView(tabOnline)
            addView(tabContainer)

            val contentContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(contentContainer)

            fun renderStats(stats: ModeStatsData) {
                contentContainer.removeAllViews()

                // Circular Win Rate Gauge
                val gauge = WinRateGaugeView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 260).apply {
                        bottomMargin = 20
                    }
                    setWinRate(stats.winRate)
                }
                contentContainer.addView(gauge)

                // Match Breakdown Row
                val statsRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 16
                    }
                    addView(createStatMetricCard("MENANG", "${stats.wins}", "#00E676"))
                    addView(createStatMetricCard("KALAH", "${stats.losses}", "#FF1744"))
                    addView(createStatMetricCard("SERI", "${stats.draws}", "#FF9100"))
                }
                contentContainer.addView(statsRow)

                // Streak Row
                val streakRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 2f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 24
                    }
                    addView(createStatMetricCard("CURRENT STREAK", "${stats.currentStreak}", "#FFD600"))
                    addView(createStatMetricCard("BEST STREAK", "${stats.bestStreak}", "#00E5FF"))
                }
                contentContainer.addView(streakRow)

                // Move Distribution Section
                val distTitle = TextView(context).apply {
                    text = getString(R.string.title_move_distribution)
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                    isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 12
                    }
                }
                contentContainer.addView(distTitle)

                val distBar = MoveDistributionBarView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 32).apply {
                        bottomMargin = 12
                    }
                    setStats(stats)
                }
                contentContainer.addView(distBar)

                // Move Labels
                val moveLabels = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 32
                    }

                    addView(createMoveStatLabel("Batu", stats.rockUsage, stats.rockPercent, "#FF5252"))
                    addView(createMoveStatLabel("Kertas", stats.paperUsage, stats.paperPercent, "#448AFF"))
                    addView(createMoveStatLabel("Gunting", stats.scissorsUsage, stats.scissorsPercent, "#69F0AE"))
                }
                contentContainer.addView(moveLabels)

                // Reset Button
                val btnReset = Button(context).apply {
                    text = getString(R.string.btn_reset_stats)
                    setTextColor(ContextCompat.getColor(context, R.color.ruby_lose))
                    setBackgroundResource(R.drawable.bg_button_danger)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    setOnClickListener {
                        NativeBridge.resetStats(isOnlineTab)
                        CustomToast.show(this@MainActivity, getString(R.string.msg_stats_reset))
                        showStatsScreen()
                    }
                }
                contentContainer.addView(btnReset)
            }

            fun loadAndRender() {
                val statsJson = NativeBridge.getStatsJson()
                val data = GameStatsData.fromJson(statsJson)
                val target = if (isOnlineTab) data.online else data.ai
                renderStats(target)
            }

            tabAi.setOnClickListener {
                isOnlineTab = false
                tabAi.setBackgroundResource(R.drawable.bg_button_primary)
                tabAi.setTextColor(Color.BLACK)
                tabOnline.setBackgroundColor(Color.TRANSPARENT)
                tabOnline.setTextColor(Color.parseColor("#94A3B8"))
                loadAndRender()
            }

            tabOnline.setOnClickListener {
                isOnlineTab = true
                tabOnline.setBackgroundResource(R.drawable.bg_button_secondary)
                tabOnline.setTextColor(Color.WHITE)
                tabAi.setBackgroundColor(Color.TRANSPARENT)
                tabAi.setTextColor(Color.parseColor("#94A3B8"))
                loadAndRender()
            }

            loadAndRender()
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createStatMetricCard(label: String, value: String, accentHex: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(16, 20, 16, 20)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                setMargins(6, 0, 6, 0)
            }

            val valText = TextView(context).apply {
                text = value
                textSize = 22f
                setTextColor(Color.parseColor(accentHex))
                isFakeBoldText = true
            }
            addView(valText)

            val labelText = TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 4
                }
            }
            addView(labelText)
        }
    }

    private fun createMoveStatLabel(name: String, count: Int, percent: Float, colorHex: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

            val text = TextView(context).apply {
                this.text = "$name ($count)"
                textSize = 12f
                setTextColor(Color.parseColor(colorHex))
                isFakeBoldText = true
            }
            addView(text)

            val pct = TextView(context).apply {
                this.text = String.format("%.1f%%", percent)
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            }
            addView(pct)
        }
    }

    // =========================================================================
    // 9. HOW TO PLAY SCREEN
    // =========================================================================
    private fun showHowToPlayScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            addView(createTopBar(getString(R.string.title_how_to_play)) { navigateTo(ScreenState.HOME) })

            // Rules Graphic Cards
            addView(createRuleCard("BATU MENGALAHKAN GUNTING", "Batu menghancurkan bilah gunting.", R.drawable.ic_rock, "#FF5252"))
            addView(createRuleCard("GUNTING MENGALAHKAN KERTAS", "Gunting membelah lembaran kertas.", R.drawable.ic_scissors, "#69F0AE"))
            addView(createRuleCard("KERTAS MENGALAHKAN BATU", "Kertas membungkus permukaan batu.", R.drawable.ic_paper, "#448AFF"))

            // AI Explanation
            val aiTitle = TextView(context).apply {
                text = "TENTANG MODE VS AI"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 24
                    bottomMargin = 8
                }
            }
            addView(aiTitle)

            val aiText = TextView(context).apply {
                text = getString(R.string.guide_ai_text)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setLineSpacing(0f, 1.3f)
            }
            addView(aiText)

            // Online Explanation
            val onlineTitle = TextView(context).apply {
                text = "TENTANG MODE ONLINE"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.neon_magenta))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 24
                    bottomMargin = 8
                }
            }
            addView(onlineTitle)

            val onlineText = TextView(context).apply {
                text = getString(R.string.guide_online_text)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setLineSpacing(0f, 1.3f)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 32
                }
            }
            addView(onlineText)
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createRuleCard(title: String, desc: String, iconRes: Int, accentHex: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 14
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48).apply { marginEnd = 20 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                val t = TextView(context).apply {
                    text = title
                    textSize = 14f
                    setTextColor(Color.parseColor(accentHex))
                    isFakeBoldText = true
                }
                addView(t)

                val d = TextView(context).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 4
                    }
                }
                addView(d)
            }
            addView(col)
        }
    }

    // =========================================================================
    // 10. FAQ SCREEN (Accordion)
    // =========================================================================
    private fun showFaqScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            addView(createTopBar(getString(R.string.title_faq)) { navigateTo(ScreenState.HOME) })

            val faqs = listOf(
                Pair(getString(R.string.faq_q1), getString(R.string.faq_a1)),
                Pair(getString(R.string.faq_q2), getString(R.string.faq_a2)),
                Pair(getString(R.string.faq_q3), getString(R.string.faq_a3)),
                Pair(getString(R.string.faq_q4), getString(R.string.faq_a4)),
                Pair(getString(R.string.faq_q5), getString(R.string.faq_a5)),
                Pair(getString(R.string.faq_q6), getString(R.string.faq_a6)),
                Pair(getString(R.string.faq_q7), getString(R.string.faq_a7))
            )

            faqs.forEach { (q, a) ->
                addView(createAccordionItem(q, a))
            }
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createAccordionItem(question: String, answer: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 14
            }

            var isExpanded = false

            val arrow = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chevron_down))
            }

            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val qText = TextView(context).apply {
                    text = question
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }
                addView(qText)
                addView(arrow)
            }
            addView(headerRow)

            val aText = TextView(context).apply {
                text = answer
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setLineSpacing(0f, 1.3f)
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 16
                }
            }
            addView(aText)

            setOnClickListener {
                isExpanded = !isExpanded
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                aText.visibility = if (isExpanded) View.VISIBLE else View.GONE
                val iconRes = if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                arrow.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
        }
    }

    // =========================================================================
    // 11. ABOUT DEVELOPER SCREEN
    // =========================================================================
    private fun showAboutScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            addView(createTopBar(getString(R.string.title_about)) { navigateTo(ScreenState.HOME) })

            // Dev Info Card
            val devCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setBackgroundResource(R.drawable.bg_card_glass)
                setPadding(36, 36, 36, 36)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 28
                    bottomMargin = 24
                }

                val avatar = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(88, 88).apply { bottomMargin = 16 }
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
                }
                addView(avatar)

                val name = TextView(context).apply {
                    text = getString(R.string.dev_name)
                    textSize = 24f
                    setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                    isFakeBoldText = true
                }
                addView(name)

                val role = TextView(context).apply {
                    text = getString(R.string.dev_role)
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.neon_magenta))
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 4
                        bottomMargin = 16
                    }
                }
                addView(role)

                val quote = TextView(context).apply {
                    text = getString(R.string.dev_quote)
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.ITALIC)
                }
                addView(quote)
            }
            addView(devCard)

            // Social Clickable Links
            val linksTitle = TextView(context).apply {
                text = "JARINGAN & SOSIAL"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
            }
            addView(linksTitle)

            addView(createSocialLinkCard("GitHub", "KyuuX444", R.drawable.ic_github, "https://github.com/KyuuX444"))
            addView(createSocialLinkCard("YouTube", "mommyykyuu", R.drawable.ic_youtube, "https://youtube.com/@momnykyuu"))
            addView(createSocialLinkCard("Instagram", "Kyzz Apis", R.drawable.ic_instagram, "https://instagram.com/kyzzapis"))
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createSocialLinkCard(platform: String, handle: String, iconRes: Int, url: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 12
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(36, 36).apply { marginEnd = 20 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

                val pText = TextView(context).apply {
                    text = platform
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    isFakeBoldText = true
                }
                addView(pText)

                val hText = TextView(context).apply {
                    text = handle
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                addView(hText)
            }
            addView(col)

            val arrow = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_copy))
            }
            addView(arrow)

            setOnClickListener {
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } catch (_: Exception) {
                    CustomToast.show(this@MainActivity, "Membuka: $url")
                }
            }
        }
    }

    // =========================================================================
    // 12. SETTINGS SCREEN
    // =========================================================================
    private fun showSettingsScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val prefs = RPSApplication.instance.preferences

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 40)

            addView(createTopBar(getString(R.string.title_settings)) { navigateTo(ScreenState.HOME) })

            // Music Toggle & Slider
            val musicRow = createSwitchRow(getString(R.string.setting_music), prefs.isMusicEnabled) { enabled ->
                prefs.isMusicEnabled = enabled
                NativeBridge.setAudioSettings(enabled, prefs.isSfxEnabled, prefs.musicVolume, prefs.sfxVolume)
                if (enabled) RPSApplication.instance.soundManager.startBgm()
                else RPSApplication.instance.soundManager.stopBgm()
            }
            addView(musicRow)

            val musicSlider = createSlider(prefs.musicVolume) { vol ->
                prefs.musicVolume = vol
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, prefs.isSfxEnabled, vol, prefs.sfxVolume)
            }
            addView(musicSlider)

            // SFX Toggle & Slider
            val sfxRow = createSwitchRow(getString(R.string.setting_sfx), prefs.isSfxEnabled) { enabled ->
                prefs.isSfxEnabled = enabled
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, enabled, prefs.musicVolume, prefs.sfxVolume)
            }
            addView(sfxRow)

            val sfxSlider = createSlider(prefs.sfxVolume) { vol ->
                prefs.sfxVolume = vol
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, prefs.isSfxEnabled, prefs.musicVolume, vol)
            }
            addView(sfxSlider)

            // Haptic Toggle & Slider
            val hapticRow = createSwitchRow(getString(R.string.setting_haptic), prefs.isHapticEnabled) { enabled ->
                prefs.isHapticEnabled = enabled
                NativeBridge.setHapticSettings(enabled, prefs.hapticIntensity)
            }
            addView(hapticRow)

            val hapticSlider = createSlider(prefs.hapticIntensity) { intensity ->
                prefs.hapticIntensity = intensity
                NativeBridge.setHapticSettings(prefs.isHapticEnabled, intensity)
            }
            addView(hapticSlider)

            // Server URL configuration
            val serverTitle = TextView(context).apply {
                text = "WEBSOCKET SERVER URL"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.neon_cyan))
                isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 28
                    bottomMargin = 8
                }
            }
            addView(serverTitle)

            val serverInput = EditText(context).apply {
                setText(prefs.serverUrl)
                setTextColor(Color.WHITE)
                textSize = 14f
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
            }
            addView(serverInput)

            val btnSaveServer = Button(context).apply {
                text = "SIMPAN SERVER URL"
                setTextColor(Color.BLACK)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 24
                }
                setOnClickListener {
                    val newUrl = serverInput.text.toString().trim()
                    if (newUrl.startsWith("ws://") || newUrl.startsWith("wss://")) {
                        prefs.serverUrl = newUrl
                        wsManager.disconnect()
                        wsManager.connect(newUrl)
                        CustomToast.show(this@MainActivity, "Server URL diperbarui!")
                    } else {
                        CustomToast.show(this@MainActivity, "URL harus diawali ws:// atau wss://", true)
                    }
                }
            }
            addView(btnSaveServer)

            // Reset Settings
            val btnReset = Button(context).apply {
                text = getString(R.string.btn_reset_settings)
                setTextColor(ContextCompat.getColor(context, R.color.ruby_lose))
                setBackgroundResource(R.drawable.bg_button_danger)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 12
                }
                setOnClickListener {
                    prefs.resetToDefaults()
                    CustomToast.show(this@MainActivity, "Pengaturan di-reset ke awal!")
                    showSettingsScreen()
                }
            }
            addView(btnReset)
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createSwitchRow(label: String, initialChecked: Boolean, onCheckedChange: (Boolean) -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 8)

            val text = TextView(context).apply {
                this.text = label
                textSize = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            }
            addView(text)

            val switch = Switch(context).apply {
                isChecked = initialChecked
                setOnCheckedChangeListener { _, checked ->
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    onCheckedChange(checked)
                }
            }
            addView(switch)
        }
    }

    private fun createSlider(initialValue: Float, onProgressChange: (Float) -> Unit): View {
        return SeekBar(this).apply {
            max = 100
            progress = (initialValue * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onProgressChange(progress / 100f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun createTopBar(title: String, onBack: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 16
            }

            val backBtn = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(36, 36).apply { marginEnd = 20 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    onBack()
                }
            }
            addView(backBtn)

            val titleView = TextView(context).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
            }
            addView(titleView)
        }
    }

    // =========================================================================
    // WebSocket Listener Callbacks (Real-time Multiplayer)
    // =========================================================================
    override fun onConnected(sessionId: String) {
        CustomToast.show(this, "Terhubung ke server multiplayer")
    }

    override fun onRoomCreated(roomCode: String) {
        navigateTo(ScreenState.ONLINE_WAITING)
    }

    override fun onRoomJoined(roomCode: String, opponentName: String) {
        navigateTo(ScreenState.ONLINE_WAITING)
    }

    override fun onMatchStart(opponentName: String, bestOf: Int) {
        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_START)
        showOnlineMatchScreen()
        onlineScoreBoard?.setNames("PLAYER", opponentName)
        onlineScoreBoard?.setupTargetDots(if (bestOf == 5) 3 else 2)
        onlineScoreBoard?.updateScore(0, 0, 1)
        onlineStatusText?.text = "Match Dimulai! Bersiaplah..."
    }

    override fun onRoundStart(roundNumber: Int, timeoutSeconds: Int) {
        selectedMoveThisRound = NativeBridge.MOVE_NONE
        onlineArenaView?.reset()
        onlineMoveCards?.resetSelection()
        onlineMoveCards?.isEnabledSelection = true
        onlineStatusText?.text = "Pilih elemen Anda! (${timeoutSeconds}s)"

        // Local countdown
        roundTimer?.cancel()
        roundTimer = object : CountDownTimer((timeoutSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                onlineStatusText?.text = "Pilih elemen Anda! (${sec}s)"
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_COUNTDOWN)
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_COUNTDOWN)
            }
            override fun onFinish() {
                onlineStatusText?.text = "Waktu Habis!"
            }
        }.start()
    }

    override fun onOpponentChoosing() {
        onlineStatusText?.text = getString(R.string.status_opponent_choosing)
    }

    override fun onOpponentChose() {
        onlineStatusText?.text = "Lawan telah memilih!"
    }

    override fun onRoundResult(
        round: Int,
        playerMove: Int,
        opponentMove: Int,
        result: Int,
        myScore: Int,
        oppScore: Int
    ) {
        roundTimer?.cancel()
        onlineMoveCards?.isEnabledSelection = false

        onlineArenaView?.startClashAnimation(playerMove, opponentMove, result) {
            onlineScoreBoard?.updateScore(myScore, oppScore, round + 1)
            when (result) {
                NativeBridge.RESULT_WIN -> {
                    onlineStatusText?.text = getString(R.string.result_win)
                    onlineStatusText?.setTextColor(ContextCompat.getColor(this, R.color.emerald_win))
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_WIN)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_WIN)
                }
                NativeBridge.RESULT_LOSE -> {
                    onlineStatusText?.text = getString(R.string.result_lose)
                    onlineStatusText?.setTextColor(ContextCompat.getColor(this, R.color.ruby_lose))
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_LOSE)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_LOSE)
                }
                else -> {
                    onlineStatusText?.text = getString(R.string.result_draw)
                    onlineStatusText?.setTextColor(ContextCompat.getColor(this, R.color.amber_draw))
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_DRAW)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_DRAW)
                }
            }
        }
    }

    override fun onMatchResult(won: Boolean, finalMyScore: Int, finalOppScore: Int) {
        roundTimer?.cancel()
        NativeBridge.recordMatchResult(
            true, finalMyScore, finalOppScore, 0,
            matchRockCount, matchPaperCount, matchScissorsCount
        )
        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MATCH_RESULT)
        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_END)

        Handler(Looper.getMainLooper()).postDelayed({
            CustomDialogs.showMatchResultDialog(
                this,
                won,
                if (won) getString(R.string.match_victory) else getString(R.string.match_defeat),
                "$finalMyScore - $finalOppScore",
                onRematch = { wsManager.sendRematchVote() },
                onExit = {
                    wsManager.sendLeaveMatch()
                    navigateTo(ScreenState.HOME)
                }
            )
        }, 800)
    }

    override fun onRematchRequested() {
        CustomToast.show(this, "Lawan meminta rematch!")
    }

    override fun onRematchAccepted() {
        CustomToast.show(this, "Rematch disetujui! Memulai match baru...")
    }

    override fun onOpponentDisconnected(gracePeriodSeconds: Int) {
        CustomToast.show(this, "Lawan terputus. Menunggu ${gracePeriodSeconds}s untuk reconnect...", true)
    }

    override fun onOpponentReconnected() {
        CustomToast.show(this, "Lawan berhasil tersambung kembali!")
    }

    override fun onOpponentLeft() {
        CustomDialogs.showErrorDialog(this, "Lawan Keluar", getString(R.string.error_opponent_disconnected)) {
            navigateTo(ScreenState.HOME)
        }
    }

    override fun onError(errorMessage: String) {
        CustomToast.show(this, errorMessage, true)
    }
}
