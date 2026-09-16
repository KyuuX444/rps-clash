package com.kyuu.rpsclash.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ModeStatsData(
    @SerializedName("totalMatches") val totalMatches: Int = 0,
    @SerializedName("wins") val wins: Int = 0,
    @SerializedName("losses") val losses: Int = 0,
    @SerializedName("draws") val draws: Int = 0,
    @SerializedName("currentStreak") val currentStreak: Int = 0,
    @SerializedName("bestStreak") val bestStreak: Int = 0,
    @SerializedName("rockUsage") val rockUsage: Int = 0,
    @SerializedName("paperUsage") val paperUsage: Int = 0,
    @SerializedName("scissorsUsage") val scissorsUsage: Int = 0,
    @SerializedName("winRate") val winRate: Float = 0.0f
) {
    val totalMoves: Int get() = rockUsage + paperUsage + scissorsUsage

    val rockPercent: Float
        get() = if (totalMoves > 0) (rockUsage.toFloat() / totalMoves) * 100f else 33.3f

    val paperPercent: Float
        get() = if (totalMoves > 0) (paperUsage.toFloat() / totalMoves) * 100f else 33.3f

    val scissorsPercent: Float
        get() = if (totalMoves > 0) (scissorsUsage.toFloat() / totalMoves) * 100f else 33.4f
}

data class GameStatsData(
    @SerializedName("ai") val ai: ModeStatsData = ModeStatsData(),
    @SerializedName("online") val online: ModeStatsData = ModeStatsData()
) {
    companion object {
        fun fromJson(json: String): GameStatsData {
            return try {
                Gson().fromJson(json, GameStatsData::class.java) ?: GameStatsData()
            } catch (e: Exception) {
                GameStatsData()
            }
        }
    }
}
