package com.kyuu.rpsclash

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

class MainActivity : AppCompatActivity(), OnlineGameListener {

    private lateinit var rootContainer: FrameLayout
    private var currentScreen: ScreenState = ScreenState.SPLASH

    // Online & Game state
    private val wsManager = WebSocketManager()
    private var vsAiBestOf = 3
    private var vsAiDifficulty = NativeBridge.DIFF_NORMAL
    private var selectedMoveThisRound = NativeBridge.MOVE_NONE
    private var roundTimer: CountDownTimer? = null
    private var currentMatchRound = 1

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
    // 1. SPLASH SCREEN (Non-neon, clean minimal)
    // =========================================================================
    private fun showSplashScreen() {
        val splashLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(48, 48, 48, 48)

            val logo = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { bottomMargin = 20 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
            }
            addView(logo)

            val animator = ValueAnimator.ofFloat(0.96f, 1.04f).apply {
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
                textSize = 30f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                letterSpacing = 0.1f
            }
            addView(title)

            val subtitle = TextView(context).apply {
                text = "THE NATIVE ARENA"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                letterSpacing = 0.15f
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 6
                    bottomMargin = 36
                }
            }
            addView(subtitle)

            val progressBar = ProgressBar(context).apply {
                indeterminateTintList = ContextCompat.getColorStateList(context, R.color.accent_primary)
            }
            addView(progressBar)

            val version = TextView(context).apply {
                text = "v1.0.0 • C++20 Core NDK"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 28
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
        }, 1500)
    }

    // =========================================================================
    // 2. HOME SCREEN (Hierarchy: Logo -> Play -> Online -> Stats -> Other)
    // =========================================================================
    private fun showHomeScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val homeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 40, 28, 32)

            // 1. Logo & Identity
            val logo = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(68, 68).apply { bottomMargin = 10 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
            }
            addView(logo)

            val title = TextView(context).apply {
                text = "RPS CLASH"
                textSize = 26f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                letterSpacing = 0.06f
            }
            addView(title)

            val tag = TextView(context).apply {
                text = "BATU • GUNTING • KERTAS"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                letterSpacing = 0.12f
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 4
                    bottomMargin = 28
                }
            }
            addView(tag)

            // 2. Play - VS AI Button
            addView(createMainPlayButton(
                title = "VS AI (OFFLINE)",
                subtitle = "Tantang AI Markov Chain",
                badgeText = "OFFLINE",
                isPrimary = true
            ) {
                navigateTo(ScreenState.VS_AI_SETUP)
            })

            // 3. Online - Multiplayer Button
            addView(createMainPlayButton(
                title = "ONLINE MULTIPLAYER",
                subtitle = "Duel Real-Time via Room",
                badgeText = "PVP",
                isPrimary = false
            ) {
                navigateTo(ScreenState.ONLINE_LOBBY)
            })

            // 4. Other Navigation (Stats, How to Play, FAQ, About, Settings)
            val navSectionTitle = TextView(context).apply {
                text = "MENU & INFORMASI"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 10
                }
            }
            addView(navSectionTitle)

            // Row 1: Statistik & Cara Bermain
            val navRow1 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 10
                }
                addView(createNavTile("Statistik", R.drawable.ic_chart) { navigateTo(ScreenState.STATS) })
                addView(createNavTile("Cara Main", R.drawable.ic_guide) { navigateTo(ScreenState.HOW_TO_PLAY) })
            }
            addView(navRow1)

            // Row 2: FAQ & Developer
            val navRow2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 10
                }
                addView(createNavTile("FAQ", R.drawable.ic_help) { navigateTo(ScreenState.FAQ) })
                addView(createNavTile("Developer", R.drawable.ic_github) { navigateTo(ScreenState.ABOUT) })
            }
            addView(navRow2)

            // Row 3: Pengaturan (Full Width)
            val navRow3 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 1f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                addView(createNavTile("Pengaturan Game", R.drawable.ic_settings) { navigateTo(ScreenState.SETTINGS) })
            }
            addView(navRow3)
        }

        scroll.addView(homeLayout)
        rootContainer.addView(scroll)
    }

    private fun createMainPlayButton(
        title: String,
        subtitle: String,
        badgeText: String,
        isPrimary: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (isPrimary) R.drawable.bg_button_primary else R.drawable.bg_card_surface)
            setPadding(22, 18, 22, 18)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 14
            }

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

                val t = TextView(context).apply {
                    text = title
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    isFakeBoldText = true
                    letterSpacing = 0.03f
                }
                addView(t)

                val s = TextView(context).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(if (isPrimary) Color.parseColor("#CBD5E1") else ContextCompat.getColor(context, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2
                    }
                }
                addView(s)
            }
            addView(col)

            val badge = TextView(context).apply {
                text = badgeText
                textSize = 10f
                isFakeBoldText = true
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(14, 5, 14, 5)
            }
            addView(badge)

            setOnClickListener {
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_BUTTON_PRESS)
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                onClick()
            }
        }
    }

    private fun createNavTile(title: String, iconRes: Int, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                setMargins(5, 0, 5, 0)
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply { marginEnd = 10 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val t = TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
            }
            addView(t)

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
            setPadding(28, 32, 28, 32)

            addView(createTopBar("PENGATURAN MATCH AI") { navigateTo(ScreenState.HOME) })

            // Difficulty Section
            val diffTitle = TextView(context).apply {
                text = "TINGKAT KESULITAN"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 20
                    bottomMargin = 10
                }
            }
            addView(diffTitle)

            var diffEasyCard: View? = null
            var diffNormalCard: View? = null
            var diffHardCard: View? = null

            fun updateDiffCards(selected: Int) {
                vsAiDifficulty = selected
                diffEasyCard?.alpha = if (selected == NativeBridge.DIFF_EASY) 1.0f else 0.45f
                diffNormalCard?.alpha = if (selected == NativeBridge.DIFF_NORMAL) 1.0f else 0.45f
                diffHardCard?.alpha = if (selected == NativeBridge.DIFF_HARD) 1.0f else 0.45f
            }

            diffEasyCard = createSelectableCard("MUDAH", "Pola acak murni, cocok untuk latihan awal", "#10B981") {
                updateDiffCards(NativeBridge.DIFF_EASY)
            }
            diffNormalCard = createSelectableCard("NORMAL", "Adaptif dengan Win-Stay Lose-Shift heuristic", "#3B82F6") {
                updateDiffCards(NativeBridge.DIFF_NORMAL)
            }
            diffHardCard = createSelectableCard("HARD (MARKOV CHAIN)", "Prediksi urutan move menggunakan N-Gram Markov Model", "#64748B") {
                updateDiffCards(NativeBridge.DIFF_HARD)
            }

            addView(diffEasyCard)
            addView(diffNormalCard)
            addView(diffHardCard)
            updateDiffCards(vsAiDifficulty)

            // Format Section
            val formatTitle = TextView(context).apply {
                text = "FORMAT PERTANDINGAN"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 10
                }
            }
            addView(formatTitle)

            var bo3Card: View? = null
            var bo5Card: View? = null

            fun updateFormatCards(bo: Int) {
                vsAiBestOf = bo
                bo3Card?.alpha = if (bo == 3) 1.0f else 0.45f
                bo5Card?.alpha = if (bo == 5) 1.0f else 0.45f
            }

            bo3Card = createSelectableCard("BEST OF 3", "Pemenang pertama yang mencapai 2 poin", "#F59E0B") {
                updateFormatCards(3)
            }
            bo5Card = createSelectableCard("BEST OF 5", "Pemenang pertama yang mencapai 3 poin", "#F97316") {
                updateFormatCards(5)
            }
            addView(bo3Card)
            addView(bo5Card)
            updateFormatCards(vsAiBestOf)

            // Start Match Button
            val btnStart = Button(context).apply {
                text = "MULAI PERTANDINGAN"
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 114).apply {
                    topMargin = 28
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
            setPadding(22, 16, 22, 16)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            val titleView = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(Color.parseColor(accentHex))
                isFakeBoldText = true
            }
            addView(titleView)

            val descView = TextView(context).apply {
                text = desc
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 3
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
    // 4. GAME SCREEN: VS AI MATCH
    // Hierarchy: Opponent -> Score -> Round / Countdown -> Arena -> Player -> Moves
    // =========================================================================
    private fun showVsAiMatchScreen() {
        NativeBridge.startNewVsAiMatch(vsAiBestOf)
        matchRockCount = 0
        matchPaperCount = 0
        matchScissorsCount = 0
        currentMatchRound = 1

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(20, 16, 20, 16)
        }

        // Top Navigation Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            val btnExit = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(30, 30)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    CustomDialogs.showErrorDialog(this@MainActivity, "Keluar", "Tinggalkan pertandingan?") {
                        navigateTo(ScreenState.HOME)
                    }
                }
            }
            addView(btnExit)

            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            }
            addView(spacer)

            val diffBadge = TextView(context).apply {
                val diffStr = when (vsAiDifficulty) {
                    NativeBridge.DIFF_EASY -> "AI: MUDAH"
                    NativeBridge.DIFF_NORMAL -> "AI: NORMAL"
                    else -> "AI: HARD"
                }
                text = diffStr
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(16, 4, 16, 4)
            }
            addView(diffBadge)
        }
        layout.addView(topBar)

        // 1 & 2 & 3. Opponent -> Big Score -> Round Status
        val targetWins = if (vsAiBestOf == 5) 3 else 2
        val scoreBoard = ScoreBoardView(this).apply {
            setNames("PLAYER", "AI BOT")
            setupTargetDots(targetWins)
            updateScore(0, 0, 1)
        }
        layout.addView(scoreBoard)

        // 4. Game Arena (Clashing Hands in battle circle)
        val arenaView = ArenaHandsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }
        layout.addView(arenaView)

        // 5. Player Info (Avatar, Name, Dots)
        val playerStatusView = scoreBoard.createPlayerStatusView()
        layout.addView(playerStatusView)

        // 6. Rock Paper Scissors Moves
        val moveCards = MoveCardsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 6
            }
        }
        layout.addView(moveCards)

        rootContainer.addView(layout)

        // Round resolution logic
        fun startVsAiRound(playerMove: Int) {
            moveCards.isEnabledSelection = false
            scoreBoard.setRoundStatus("MEMILIH ELEMEN...")

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
                        scoreBoard.setRoundStatus("MENANG RONDE INI!", "#10B981")
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_WIN)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_WIN)
                    }
                    NativeBridge.RESULT_LOSE -> {
                        scoreBoard.setRoundStatus("KALAH RONDE INI!", "#EF4444")
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_LOSE)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_LOSE)
                    }
                    else -> {
                        scoreBoard.setRoundStatus("RONDE SERI!", "#F59E0B")
                        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_DRAW)
                        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_DRAW)
                    }
                }

                scoreBoard.updateScore(pScore, aiScore, nextRound)
                currentMatchRound = nextRound

                if (isGameOver) {
                    val won = pScore > aiScore
                    val matchRes = if (won) NativeBridge.RESULT_WIN else NativeBridge.RESULT_LOSE

                    NativeBridge.recordMatchResult(
                        false, pScore, aiScore, 0,
                        matchRockCount, matchPaperCount, matchScissorsCount
                    )
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MATCH_RESULT)
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_END)

                    Handler(Looper.getMainLooper()).postDelayed({
                        CustomDialogs.showMatchResultDialog(
                            this,
                            matchRes,
                            "$pScore - $aiScore",
                            currentMatchRound,
                            onRematch = { showVsAiMatchScreen() },
                            onExit = { navigateTo(ScreenState.HOME) }
                        )
                    }, 650)
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        arenaView.reset()
                        moveCards.resetSelection()
                        moveCards.isEnabledSelection = true
                        scoreBoard.setRoundStatus("PILIH ELEMEN ANDA")
                    }, 1200)
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
            setPadding(28, 32, 28, 32)

            addView(createTopBar("ONLINE MULTIPLAYER") { navigateTo(ScreenState.HOME) })

            // Connection Indicator Bar
            val connBar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(18, 12, 18, 12)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 20
                }

                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(14, 14).apply { marginEnd = 10 }
                    val colorHex = if (wsManager.state == ConnectionState.CONNECTED) "#10B981" else "#F59E0B"
                    val shape = GradientDrawable().apply {
                        this.shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(colorHex))
                    }
                    background = shape
                }
                addView(dot)

                val status = TextView(context).apply {
                    text = when (wsManager.state) {
                        ConnectionState.CONNECTED -> "Online • ${RPSApplication.instance.preferences.serverUrl}"
                        ConnectionState.CONNECTING -> "Menghubungkan ke server..."
                        ConnectionState.RECONNECTING -> "Menyambung ulang..."
                        else -> "Klik untuk koneksi ke server"
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
            addView(connBar)

            if (wsManager.state == ConnectionState.DISCONNECTED) {
                wsManager.connect(RPSApplication.instance.preferences.serverUrl)
            }

            // Quick Match Action
            addView(createMainPlayButton(
                title = "QUICK MATCH",
                subtitle = "Cari lawan langsung secara otomatis",
                badgeText = "INSTANT",
                isPrimary = true
            ) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createMainPlayButton
                }
                wsManager.sendQuickMatch()
                navigateTo(ScreenState.ONLINE_WAITING)
            })

            // Create Room Action
            addView(createMainPlayButton(
                title = "BUAT ROOM",
                subtitle = "Buat room baru dan bagikan kode ke teman",
                badgeText = "HOST",
                isPrimary = false
            ) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createMainPlayButton
                }
                wsManager.sendCreateRoom(3)
            })

            // Join Room Action
            addView(createNavTile("Gabung Room dengan Kode", R.drawable.ic_vs) {
                if (wsManager.state != ConnectionState.CONNECTED) {
                    CustomToast.show(this@MainActivity, "Menghubungkan ke server...", true)
                    wsManager.connect(RPSApplication.instance.preferences.serverUrl)
                    return@createNavTile
                }
                CustomDialogs.showJoinRoomDialog(this@MainActivity) { code ->
                    wsManager.sendJoinRoom(code)
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 4
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
            setPadding(28, 32, 28, 32)
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
                indeterminateTintList = ContextCompat.getColorStateList(context, R.color.accent_primary)
                layoutParams = LinearLayout.LayoutParams(56, 56).apply { bottomMargin = 16 }
            }
            addView(spinner)

            val waitingMsg = TextView(context).apply {
                text = "Mencari Lawan Tanding..."
                textSize = 15f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                gravity = Gravity.CENTER
            }
            addView(waitingMsg)

            // Room code display
            val code = wsManager.currentRoomCode
            if (code != null) {
                val codeCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_card_surface)
                    setPadding(28, 20, 28, 20)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        topMargin = 24
                        bottomMargin = 18
                    }

                    val label = TextView(context).apply {
                        text = "KODE ROOM ANDA"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                        isFakeBoldText = true
                        letterSpacing = 0.08f
                    }
                    addView(label)

                    val codeText = TextView(context).apply {
                        text = code
                        textSize = 32f
                        setTextColor(Color.WHITE)
                        isFakeBoldText = true
                        letterSpacing = 0.18f
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            topMargin = 4
                            bottomMargin = 14
                        }
                    }
                    addView(codeText)

                    val btnCopy = Button(context).apply {
                        text = "SALIN KODE"
                        setTextColor(Color.WHITE)
                        isFakeBoldText = true
                        setBackgroundResource(R.drawable.bg_button_primary)
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, 96).apply {
                            setPadding(28, 0, 28, 0)
                        }
                        setOnClickListener {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("RPS Room Code", code))
                            CustomToast.show(this@MainActivity, "Kode room disalin ke clipboard!")
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
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 110)
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
    // 7. GAME SCREEN: ONLINE MATCH
    // Hierarchy: Opponent -> Score -> Round / Countdown -> Arena -> Player -> Moves
    // =========================================================================
    private var onlineArenaView: ArenaHandsView? = null
    private var onlineScoreBoard: ScoreBoardView? = null
    private var onlineMoveCards: MoveCardsView? = null

    private fun showOnlineMatchScreen() {
        currentMatchRound = 1
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(20, 16, 20, 16)
        }

        // Top Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            val btnExit = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(30, 30)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    CustomDialogs.showErrorDialog(this@MainActivity, "Keluar", "Keluar dari match online?") {
                        wsManager.sendLeaveMatch()
                        navigateTo(ScreenState.HOME)
                    }
                }
            }
            addView(btnExit)

            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            }
            addView(spacer)

            val roomBadge = TextView(context).apply {
                text = "ROOM: ${wsManager.currentRoomCode ?: "PVP"}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(16, 4, 16, 4)
            }
            addView(roomBadge)
        }
        layout.addView(topBar)

        // 1 & 2 & 3. Opponent -> Big Score -> Round Status
        onlineScoreBoard = ScoreBoardView(this).apply {
            setNames("PLAYER", "LAWAN")
            setupTargetDots(2)
            updateScore(0, 0, 1)
        }
        layout.addView(onlineScoreBoard)

        // 4. Game Arena
        onlineArenaView = ArenaHandsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }
        layout.addView(onlineArenaView)

        // 5. Player Info (Avatar, Name, Dots)
        val playerStatusView = onlineScoreBoard!!.createPlayerStatusView()
        layout.addView(playerStatusView)

        // 6. Rock Paper Scissors Moves
        onlineMoveCards = MoveCardsView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 6
            }
            onMoveSelectedListener = { move ->
                selectedMoveThisRound = move
                isEnabledSelection = false
                onlineScoreBoard?.setRoundStatus("MOVE TERKIRIM • MENUNGGU LAWAN...")
                wsManager.sendSubmitMove(move)
            }
        }
        layout.addView(onlineMoveCards)

        rootContainer.addView(layout)
    }

    // =========================================================================
    // 8. STATS SCREEN (Main Statistic -> Match Record -> Streak -> Move Usage)
    // =========================================================================
    private fun showStatsScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)

            addView(createTopBar("STATISTIK GAME") { navigateTo(ScreenState.HOME) })

            // Tab Selector Pill
            var isOnlineTab = false
            val tabContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_pill)
                setPadding(4, 4, 4, 4)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 16
                    bottomMargin = 18
                }
            }

            val tabAi = Button(context).apply {
                text = "VS AI"
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(0, 96, 1f)
            }

            val tabOnline = Button(context).apply {
                text = "ONLINE"
                setTextColor(Color.parseColor("#94A3B8"))
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(0, 96, 1f)
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

                // 1. Main Statistic: Win Rate Gauge
                val gauge = WinRateGaugeView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 220).apply {
                        bottomMargin = 8
                    }
                    setWinRate(stats.winRate)
                }
                contentContainer.addView(gauge)

                val totalMatchesPill = TextView(context).apply {
                    text = "TOTAL PERTANDINGAN: ${stats.totalMatches}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#94A3B8"))
                    isFakeBoldText = true
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 18
                    }
                }
                contentContainer.addView(totalMatchesPill)

                // 2. Match Record: Menang, Kalah, Seri
                val statsRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 14
                    }
                    addView(createStatMetricCard("MENANG", "${stats.wins}", "#10B981"))
                    addView(createStatMetricCard("KALAH", "${stats.losses}", "#EF4444"))
                    addView(createStatMetricCard("SERI", "${stats.draws}", "#F59E0B"))
                }
                contentContainer.addView(statsRow)

                // 3. Streak Record: Current Streak & Best Streak
                val streakRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 2f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 20
                    }
                    addView(createStatMetricCard("CURRENT STREAK", "${stats.currentStreak}", "#F59E0B"))
                    addView(createStatMetricCard("BEST STREAK", "${stats.bestStreak}", "#3B82F6"))
                }
                contentContainer.addView(streakRow)

                // 4. Move Usage Section
                val distTitle = TextView(context).apply {
                    text = "MOVE USAGE DISTRIBUTION"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                    isFakeBoldText = true
                    letterSpacing = 0.08f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 8
                    }
                }
                contentContainer.addView(distTitle)

                val distBar = MoveDistributionBarView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 24).apply {
                        bottomMargin = 10
                    }
                    setStats(stats)
                }
                contentContainer.addView(distBar)

                val moveLabels = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        bottomMargin = 24
                    }

                    addView(createMoveStatLabel("Batu", stats.rockUsage, stats.rockPercent, "#E11D48"))
                    addView(createMoveStatLabel("Kertas", stats.paperUsage, stats.paperPercent, "#2563EB"))
                    addView(createMoveStatLabel("Gunting", stats.scissorsUsage, stats.scissorsPercent, "#059669"))
                }
                contentContainer.addView(moveLabels)

                // Reset Button
                val btnReset = Button(context).apply {
                    text = "RESET STATISTIK"
                    setTextColor(ContextCompat.getColor(context, R.color.state_lose))
                    setBackgroundResource(R.drawable.bg_button_danger)
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 110)
                    setOnClickListener {
                        NativeBridge.resetStats(isOnlineTab)
                        CustomToast.show(this@MainActivity, "Statistik telah di-reset!")
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
                tabAi.setTextColor(Color.WHITE)
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
            setPadding(14, 16, 14, 16)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }

            val valText = TextView(context).apply {
                text = value
                textSize = 20f
                setTextColor(Color.parseColor(accentHex))
                isFakeBoldText = true
            }
            addView(valText)

            val labelText = TextView(context).apply {
                text = label
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                letterSpacing = 0.05f
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 3
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
    // 9. HOW TO PLAY SCREEN (Visual Relationship Cycle & Minimal Rules)
    // =========================================================================
    private fun showHowToPlayScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)

            addView(createTopBar("CARA BERMAIN") { navigateTo(ScreenState.HOME) })

            // Visual Cycle Header
            val cycleTitle = TextView(context).apply {
                text = "SIKLUS HUBUNGAN ELEMEN"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 12
                }
            }
            addView(cycleTitle)

            // Cycle Visual Flow
            val cycleCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(18, 18, 18, 18)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 14
                }

                fun createCycleIcon(iconRes: Int, label: String, colorHex: String): View {
                    return LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        val iv = ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(38, 38)
                            setImageDrawable(ContextCompat.getDrawable(context, iconRes))
                        }
                        addView(iv)
                        val tv = TextView(context).apply {
                            text = label
                            textSize = 11f
                            setTextColor(Color.parseColor(colorHex))
                            isFakeBoldText = true
                            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = 4 }
                        }
                        addView(tv)
                    }
                }

                fun createArrow(): View {
                    return TextView(context).apply {
                        text = "→"
                        textSize = 18f
                        setTextColor(Color.parseColor("#64748B"))
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            setMargins(12, 0, 12, 0)
                        }
                    }
                }

                addView(createCycleIcon(R.drawable.ic_rock, "Batu", "#E11D48"))
                addView(createArrow())
                addView(createCycleIcon(R.drawable.ic_scissors, "Gunting", "#059669"))
                addView(createArrow())
                addView(createCycleIcon(R.drawable.ic_paper, "Kertas", "#2563EB"))
            }
            addView(cycleCard)

            // Short Rules
            addView(createRuleCard("BATU > GUNTING", "Batu menghancurkan bilah gunting.", R.drawable.ic_rock, "#E11D48"))
            addView(createRuleCard("GUNTING > KERTAS", "Gunting membelah lembaran kertas.", R.drawable.ic_scissors, "#059669"))
            addView(createRuleCard("KERTAS > BATU", "Kertas membungkus permukaan batu.", R.drawable.ic_paper, "#2563EB"))

            // Match Scoring Rules
            val formatTitle = TextView(context).apply {
                text = "SISTEM KEMENANGAN"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 8
                }
            }
            addView(formatTitle)

            val formatCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(18, 14, 18, 14)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 20
                }

                val b1 = TextView(context).apply {
                    text = "• Best of 3: Pemenang pertama yang meraih 2 poin menang match."
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setLineSpacing(0f, 1.2f)
                }
                addView(b1)

                val b2 = TextView(context).apply {
                    text = "• Best of 5: Pemenang pertama yang meraih 3 poin menang match."
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    setLineSpacing(0f, 1.2f)
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 4
                    }
                }
                addView(b2)
            }
            addView(formatCard)
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createRuleCard(title: String, desc: String, iconRes: Int, accentHex: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_surface)
            setPadding(18, 14, 18, 14)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(34, 34).apply { marginEnd = 14 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                val t = TextView(context).apply {
                    text = title
                    textSize = 13f
                    setTextColor(Color.parseColor(accentHex))
                    isFakeBoldText = true
                }
                addView(t)

                val d = TextView(context).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 2
                    }
                }
                addView(d)
            }
            addView(col)
        }
    }

    // =========================================================================
    // 10. FAQ SCREEN (Smooth Native Accordion)
    // =========================================================================
    private fun showFaqScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)

            addView(createTopBar("FAQ") { navigateTo(ScreenState.HOME) })

            val faqs = listOf(
                Pair("Bagaimana cara AI bekerja?", "AI menggunakan kombinasi heuristik Win-Stay Lose-Shift pada tingkat Normal, dan N-Gram Markov Chain pada tingkat Hard untuk memprediksi pola langkah Anda secara real-time."),
                Pair("Apakah mode VS AI membutuhkan internet?", "Tidak. Seluruh engine game dan algoritma AI dikompilasi secara native dalam C++20 dan berjalan 100% offline tanpa koneksi data."),
                Pair("Bagaimana cara bermain online dengan teman?", "Pilih Online Multiplayer → Buat Room. Bagikan 6 karakter kode room kepada teman Anda. Teman Anda cukup memilih Gabung Room dan memasukkan kode tersebut."),
                Pair("Apa yang terjadi jika koneksi terputus saat PvP?", "Sistem menyediakan grace period selama 10 detik untuk melakukan auto-reconnect secara otomatis sebelum ronde dianggap walkover."),
                Pair("Apakah game ini mendukung semua ponsel Android?", "Ya! Aplikasi dikompilasi untuk 4 ABI (arm64-v8a, armeabi-v7a, x86, x86_64) dan mendukung Android 5.0 Lollipop hingga Android 15+."),
                Pair("Bagaimana data statistik disimpan?", "Statistik kemenangan, kekalahan, streak, dan distribusi elemen disimpan secara persisten di penyimpanan lokal perangkat."),
                Pair("Apakah saya bisa mengganti URL server multiplayer?", "Bisa. Buka menu Pengaturan, ubah URL WebSocket pada kolom Server URL, lalu tekan Simpan.")
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
            setPadding(18, 14, 18, 14)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            var isExpanded = false

            val arrow = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(20, 20)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chevron_down))
            }

            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val qText = TextView(context).apply {
                    text = question
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = 10 }
                }
                addView(qText)
                addView(arrow)
            }
            addView(headerRow)

            val aText = TextView(context).apply {
                text = answer
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setLineSpacing(0f, 1.25f)
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 10
                }
            }
            addView(aText)

            setOnClickListener {
                isExpanded = !isExpanded
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)

                // Smooth rotation of chevron
                val targetRot = if (isExpanded) 180f else 0f
                ObjectAnimator.ofFloat(arrow, View.ROTATION, targetRot).setDuration(180).start()

                // Smooth expand / collapse
                aText.visibility = if (isExpanded) View.VISIBLE else View.GONE
            }
        }
    }

    // =========================================================================
    // 11. ABOUT DEVELOPER SCREEN (Minimal & Exact Links)
    // =========================================================================
    private fun showAboutScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)

            addView(createTopBar("ABOUT") { navigateTo(ScreenState.HOME) })

            // Dev Profile Card
            val devCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = 18
                    bottomMargin = 20
                }

                val avatar = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(64, 64).apply { bottomMargin = 12 }
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_logo))
                }
                addView(avatar)

                val name = TextView(context).apply {
                    text = "Kyuu"
                    textSize = 20f
                    setTextColor(Color.WHITE)
                    isFakeBoldText = true
                }
                addView(name)

                val role = TextView(context).apply {
                    text = "Developer & System Architect"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        topMargin = 3
                        bottomMargin = 10
                    }
                }
                addView(role)

                val quote = TextView(context).apply {
                    text = "\"Crafting native performance with modern C++ and minimal design.\""
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.ITALIC)
                }
                addView(quote)
            }
            addView(devCard)

            // Social Section
            val linksTitle = TextView(context).apply {
                text = "JARINGAN & SOSIAL"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
                isFakeBoldText = true
                letterSpacing = 0.08f
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 8
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
            setPadding(18, 14, 18, 14)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = 8
            }

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).apply { marginEnd = 14 }
                setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            }
            addView(icon)

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

                val pText = TextView(context).apply {
                    text = platform
                    textSize = 13f
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
                layoutParams = LinearLayout.LayoutParams(18, 18)
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
    // 12. SETTINGS SCREEN (Grouped: Audio, Haptic, Gameplay, Other)
    // =========================================================================
    private fun showSettingsScreen() {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
        }

        val prefs = RPSApplication.instance.preferences

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)

            addView(createTopBar("PENGATURAN") { navigateTo(ScreenState.HOME) })

            // Group 1: Audio
            addView(createSettingsCategoryTitle("AUDIO"))

            addView(createSwitchRow("Musik Latar (BGM)", prefs.isMusicEnabled) { enabled ->
                prefs.isMusicEnabled = enabled
                NativeBridge.setAudioSettings(enabled, prefs.isSfxEnabled, prefs.musicVolume, prefs.sfxVolume)
                if (enabled) RPSApplication.instance.soundManager.startBgm()
                else RPSApplication.instance.soundManager.stopBgm()
            })

            addView(createSlider(prefs.musicVolume) { vol ->
                prefs.musicVolume = vol
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, prefs.isSfxEnabled, vol, prefs.sfxVolume)
            })

            addView(createSwitchRow("Efek Suara (SFX)", prefs.isSfxEnabled) { enabled ->
                prefs.isSfxEnabled = enabled
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, enabled, prefs.musicVolume, prefs.sfxVolume)
            })

            addView(createSlider(prefs.sfxVolume) { vol ->
                prefs.sfxVolume = vol
                NativeBridge.setAudioSettings(prefs.isMusicEnabled, prefs.isSfxEnabled, prefs.musicVolume, vol)
            })

            // Group 2: Haptic
            addView(createSettingsCategoryTitle("HAPTIC FEEDBACK"))

            addView(createSwitchRow("Getaran Haptic", prefs.isHapticEnabled) { enabled ->
                prefs.isHapticEnabled = enabled
                NativeBridge.setHapticSettings(enabled, prefs.hapticIntensity)
            })

            addView(createSlider(prefs.hapticIntensity) { intensity ->
                prefs.hapticIntensity = intensity
                NativeBridge.setHapticSettings(prefs.isHapticEnabled, intensity)
            })

            // Group 3: Gameplay & Server
            addView(createSettingsCategoryTitle("WEBSOCKET SERVER"))

            val serverInput = EditText(context).apply {
                setText(prefs.serverUrl)
                setTextColor(Color.WHITE)
                textSize = 13f
                setBackgroundResource(R.drawable.bg_card_surface)
                setPadding(18, 14, 18, 14)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    bottomMargin = 8
                }
            }
            addView(serverInput)

            val btnSaveServer = Button(context).apply {
                text = "SIMPAN SERVER URL"
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                setBackgroundResource(R.drawable.bg_button_primary)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 106).apply {
                    bottomMargin = 14
                }
                setOnClickListener {
                    val newUrl = serverInput.text.toString().trim()
                    if (newUrl.startsWith("ws://") || newUrl.startsWith("wss://")) {
                        prefs.serverUrl = newUrl
                        wsManager.disconnect()
                        wsManager.connect(newUrl)
                        CustomToast.show(this@MainActivity, "Server URL berhasil diperbarui!")
                    } else {
                        CustomToast.show(this@MainActivity, "URL harus diawali ws:// atau wss://", true)
                    }
                }
            }
            addView(btnSaveServer)

            // Group 4: Other
            addView(createSettingsCategoryTitle("LAINNYA"))

            val btnReset = Button(context).apply {
                text = "RESET PENGATURAN"
                setTextColor(ContextCompat.getColor(context, R.color.state_lose))
                setBackgroundResource(R.drawable.bg_button_danger)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 106).apply {
                    bottomMargin = 8
                }
                setOnClickListener {
                    prefs.resetToDefaults()
                    CustomToast.show(this@MainActivity, "Pengaturan di-reset ke default!")
                    showSettingsScreen()
                }
            }
            addView(btnReset)

            val btnAbout = Button(context).apply {
                text = "TENTANG PENGEMBANG"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_card_surface)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 106)
                setOnClickListener {
                    navigateTo(ScreenState.ABOUT)
                }
            }
            addView(btnAbout)
        }

        scroll.addView(layout)
        rootContainer.addView(scroll)
    }

    private fun createSettingsCategoryTitle(title: String): View {
        return TextView(this).apply {
            text = title
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
            isFakeBoldText = true
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = 16
                bottomMargin = 8
            }
        }
    }

    private fun createSwitchRow(label: String, initialChecked: Boolean, onCheckedChange: (Boolean) -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 4)

            val text = TextView(context).apply {
                this.text = label
                textSize = 13f
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
                bottomMargin = 10
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
                bottomMargin = 10
            }

            val backBtn = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(30, 30).apply { marginEnd = 14 }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back))
                setOnClickListener {
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_TAP)
                    onBack()
                }
            }
            addView(backBtn)

            val titleView = TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(Color.WHITE)
                isFakeBoldText = true
                letterSpacing = 0.04f
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
        onlineScoreBoard?.setRoundStatus("MATCH DIMULAI • BERSIAPLAH")
    }

    override fun onRoundStart(roundNumber: Int, timeoutSeconds: Int) {
        selectedMoveThisRound = NativeBridge.MOVE_NONE
        onlineArenaView?.reset()
        onlineMoveCards?.resetSelection()
        onlineMoveCards?.isEnabledSelection = true
        onlineScoreBoard?.setRoundStatus("PILIH ELEMEN (${timeoutSeconds}s)")

        // Local countdown
        roundTimer?.cancel()
        roundTimer = object : CountDownTimer((timeoutSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                onlineScoreBoard?.setRoundStatus("PILIH ELEMEN (${sec}s)")
                RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_COUNTDOWN)
                RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_COUNTDOWN)
            }
            override fun onFinish() {
                onlineScoreBoard?.setRoundStatus("WAKTU HABIS!")
            }
        }.start()
    }

    override fun onOpponentChoosing() {
        onlineScoreBoard?.setRoundStatus("LAWAN SEDANG MEMILIH...")
    }

    override fun onOpponentChose() {
        onlineScoreBoard?.setRoundStatus("LAWAN TELAH MEMILIH!")
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
        currentMatchRound = round

        onlineArenaView?.startClashAnimation(playerMove, opponentMove, result) {
            onlineScoreBoard?.updateScore(myScore, oppScore, round + 1)
            when (result) {
                NativeBridge.RESULT_WIN -> {
                    onlineScoreBoard?.setRoundStatus("MENANG RONDE INI!", "#10B981")
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_WIN)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_WIN)
                }
                NativeBridge.RESULT_LOSE -> {
                    onlineScoreBoard?.setRoundStatus("KALAH RONDE INI!", "#EF4444")
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_LOSE)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_LOSE)
                }
                else -> {
                    onlineScoreBoard?.setRoundStatus("RONDE SERI!", "#F59E0B")
                    RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_DRAW)
                    RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_DRAW)
                }
            }
        }
    }

    override fun onMatchResult(won: Boolean, finalMyScore: Int, finalOppScore: Int) {
        roundTimer?.cancel()
        val resultState = if (won) NativeBridge.RESULT_WIN else NativeBridge.RESULT_LOSE

        NativeBridge.recordMatchResult(
            true, finalMyScore, finalOppScore, 0,
            matchRockCount, matchPaperCount, matchScissorsCount
        )
        RPSApplication.instance.hapticManager.trigger(NativeBridge.HAPTIC_MATCH_RESULT)
        RPSApplication.instance.soundManager.playSfx(NativeBridge.SFX_MATCH_END)

        Handler(Looper.getMainLooper()).postDelayed({
            CustomDialogs.showMatchResultDialog(
                this,
                resultState,
                "$finalMyScore - $finalOppScore",
                currentMatchRound,
                onRematch = { wsManager.sendRematchVote() },
                onExit = {
                    wsManager.sendLeaveMatch()
                    navigateTo(ScreenState.HOME)
                }
            )
        }, 650)
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
        CustomDialogs.showErrorDialog(this, "Lawan Keluar", "Lawan telah meninggalkan pertandingan.") {
            navigateTo(ScreenState.HOME)
        }
    }

    override fun onError(errorMessage: String) {
        CustomToast.show(this, errorMessage, true)
    }
}
