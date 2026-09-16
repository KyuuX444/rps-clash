#include "GameCore.hpp"

namespace rps::game {

Result calculateResult(Move player, Move opponent) {
    if (player == Move::None && opponent == Move::None) return Result::Draw;
    if (player == Move::None) return Result::Lose; // player timed out
    if (opponent == Move::None) return Result::Win; // opponent timed out

    if (player == opponent) {
        return Result::Draw;
    }

    if ((player == Move::Rock && opponent == Move::Scissors) ||
        (player == Move::Paper && opponent == Move::Rock) ||
        (player == Move::Scissors && opponent == Move::Paper)) {
        return Result::Win;
    }

    return Result::Lose;
}

MatchManager::MatchManager() {
    reset(3);
}

void MatchManager::reset(int bestOfRounds) {
    mBestOf = (bestOfRounds == 5) ? 5 : 3;
    mTargetWins = (mBestOf == 5) ? 3 : 2;
    mCurrentRound = 1;
    mPlayerScore = 0;
    mOpponentScore = 0;
    mStatus = MatchStatus::InProgress;
    mHistory.clear();
}

RoundRecord MatchManager::playRound(Move playerMove, Move opponentMove) {
    Result res = calculateResult(playerMove, opponentMove);

    RoundRecord record{
        mCurrentRound,
        playerMove,
        opponentMove,
        res
    };
    mHistory.push_back(record);

    if (res == Result::Win) {
        mPlayerScore++;
    } else if (res == Result::Lose) {
        mOpponentScore++;
    }
    // Draw does not advance score

    if (mPlayerScore >= mTargetWins) {
        mStatus = MatchStatus::PlayerWon;
    } else if (mOpponentScore >= mTargetWins) {
        mStatus = MatchStatus::OpponentWon;
    } else {
        mCurrentRound++;
    }

    return record;
}

std::vector<Move> MatchManager::getPlayerMoveHistory() const {
    std::vector<Move> moves;
    moves.reserve(mHistory.size());
    for (const auto& r : mHistory) {
        if (r.playerMove != Move::None) {
            moves.push_back(r.playerMove);
        }
    }
    return moves;
}

std::vector<Move> MatchManager::getOpponentMoveHistory() const {
    std::vector<Move> moves;
    moves.reserve(mHistory.size());
    for (const auto& r : mHistory) {
        if (r.opponentMove != Move::None) {
            moves.push_back(r.opponentMove);
        }
    }
    return moves;
}

} // namespace rps::game
