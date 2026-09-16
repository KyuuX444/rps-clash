package com.kyuu.rpsclash.nativebridge

object NativeBridge {

    init {
        System.loadLibrary("gamecore")
    }

    // Move enum constants
    const val MOVE_ROCK = 0
    const val MOVE_PAPER = 1
    const val MOVE_SCISSORS = 2
    const val MOVE_NONE = -1

    // Result enum constants
    const val RESULT_WIN = 0
    const val RESULT_LOSE = 1
    const val RESULT_DRAW = 2
    const val RESULT_NONE = -1

    // Difficulty constants
    const val DIFF_EASY = 0
    const val DIFF_NORMAL = 1
    const val DIFF_HARD = 2

    // Haptic pattern constants
    const val HAPTIC_BUTTON_PRESS = 0
    const val HAPTIC_MOVE_SELECTION = 1
    const val HAPTIC_COUNTDOWN = 2
    const val HAPTIC_WIN = 3
    const val HAPTIC_LOSE = 4
    const val HAPTIC_DRAW = 5
    const val HAPTIC_MATCH_RESULT = 6

    // Sound effect constants
    const val SFX_TAP = 0
    const val SFX_SELECT = 1
    const val SFX_COUNTDOWN = 2
    const val SFX_MATCH_START = 3
    const val SFX_WIN = 4
    const val SFX_LOSE = 5
    const val SFX_DRAW = 6
    const val SFX_MATCH_END = 7

    external fun initCore(storageDir: String)
    external fun calculateResult(playerMove: Int, opponentMove: Int): Int
    external fun generateAIMove(difficulty: Int, playerHistory: IntArray, aiHistory: IntArray): Int
    external fun startNewVsAiMatch(bestOf: Int)
    external fun playVsAiRound(playerMove: Int, difficulty: Int): IntArray
    external fun getStatsJson(): String
    external fun recordMatchResult(isOnline: Boolean, wins: Int, losses: Int, draws: Int, rock: Int, paper: Int, scissors: Int)
    external fun resetStats(isOnline: Boolean)
    external fun resetAllStats()
    external fun setAudioSettings(musicEnabled: Boolean, sfxEnabled: Boolean, musicVol: Float, sfxVol: Float)
    external fun setHapticSettings(enabled: Boolean, intensity: Float)
    external fun triggerHaptic(pattern: Int)
    external fun playSound(sfx: Int)
}
