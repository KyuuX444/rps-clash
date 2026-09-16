#pragma once

#include "Move.hpp"
#include "Result.hpp"
#include <vector>
#include <string>

namespace rps::game {

enum class MatchStatus {
    InProgress,
    PlayerWon,
    OpponentWon
};

struct RoundRecord {
    int roundNumber;
    Move playerMove;
    Move opponentMove;
    Result result;
};

// Core pure function requested by user
Result calculateResult(Move player, Move opponent);

class MatchManager {
public:
    MatchManager();
    void reset(int bestOfRounds = 3); // 3 for Best of 3 (target: 2), 5 for Best of 5 (target: 3)

    RoundRecord playRound(Move playerMove, Move opponentMove);

    int getTargetWins() const { return mTargetWins; }
    int getBestOf() const { return mBestOf; }
    int getRoundNumber() const { return mCurrentRound; }
    int getPlayerScore() const { return mPlayerScore; }
    int getOpponentScore() const { return mOpponentScore; }
    MatchStatus getStatus() const { return mStatus; }
    bool isMatchOver() const { return mStatus != MatchStatus::InProgress; }

    const std::vector<RoundRecord>& getHistory() const { return mHistory; }
    std::vector<Move> getPlayerMoveHistory() const;
    std::vector<Move> getOpponentMoveHistory() const;

private:
    int mBestOf;
    int mTargetWins;
    int mCurrentRound;
    int mPlayerScore;
    int mOpponentScore;
    MatchStatus mStatus;
    std::vector<RoundRecord> mHistory;
};

} // namespace rps::game
