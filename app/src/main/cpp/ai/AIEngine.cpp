#include "AIEngine.hpp"
#include "../utils/Random.hpp"
#include <map>
#include <algorithm>

namespace rps::ai {

using namespace rps::game;
using namespace rps::utils;

AIEngine& AIEngine::getInstance() {
    static AIEngine instance;
    return instance;
}

Move AIEngine::getRandomMove() {
    int val = Random::getInstance().nextInt(0, 2);
    return static_cast<Move>(val);
}

Move AIEngine::chooseEasy(
    const std::vector<Move>& playerHistory,
    const std::vector<Move>& /*aiHistory*/
) {
    // Easy mode: 65% pure random, 35% repeat player's last move (highly exploitable)
    if (playerHistory.empty() || Random::getInstance().chance(0.65f)) {
        return getRandomMove();
    }
    return playerHistory.back();
}

Move AIEngine::chooseNormal(
    const std::vector<Move>& playerHistory,
    const std::vector<Move>& aiHistory
) {
    if (playerHistory.empty()) {
        // Most casual human players start with Rock (36%), so playing Paper counters Rock.
        // Add 30% randomness.
        if (Random::getInstance().chance(0.70f)) {
            return Move::Paper;
        }
        return getRandomMove();
    }

    // 20% randomness to prevent being completely predictable
    if (Random::getInstance().chance(0.20f)) {
        return getRandomMove();
    }

    Move lastPlayerMove = playerHistory.back();
    Move lastAIMove = aiHistory.empty() ? Move::None : aiHistory.back();

    // Psychology: Win-Stay Lose-Shift heuristic
    if (lastAIMove != Move::None) {
        Result lastResult = calculateResult(lastPlayerMove, lastAIMove);

        if (lastResult == Result::Win) {
            // Player won: high probability (60%) they will stay with the same move.
            // AI plays the move that beats player's winning move.
            return getWinningMoveAgainst(lastPlayerMove);
        } else if (lastResult == Result::Lose) {
            // Player lost: high probability they shift to the move that beats AI's winning move.
            Move predictedPlayerMove = getWinningMoveAgainst(lastAIMove);
            return getWinningMoveAgainst(predictedPlayerMove);
        } else {
            // Draw: players frequently shift to the move that would have beaten the draw move.
            Move predictedPlayerMove = getWinningMoveAgainst(lastPlayerMove);
            return getWinningMoveAgainst(predictedPlayerMove);
        }
    }

    return getWinningMoveAgainst(lastPlayerMove);
}

Move AIEngine::chooseHard(
    const std::vector<Move>& playerHistory,
    const std::vector<Move>& aiHistory
) {
    if (playerHistory.size() < 2) {
        return chooseNormal(playerHistory, aiHistory);
    }

    // Markov Chain (1st order & 2nd order transition matrix)
    Move lastPlayerMove = playerHistory.back();

    // Frequency array: counts how often player moved to Rock (0), Paper (1), Scissors (2)
    // after having just played `lastPlayerMove`.
    std::array<int, 3> transitionCounts = {0, 0, 0};

    for (size_t i = 1; i < playerHistory.size(); ++i) {
        if (playerHistory[i - 1] == lastPlayerMove) {
            int nextMoveIdx = static_cast<int>(playerHistory[i]);
            if (nextMoveIdx >= 0 && nextMoveIdx < 3) {
                transitionCounts[nextMoveIdx]++;
            }
        }
    }

    // Also look for cycle patterns: (R->P->S or R->S->P)
    int cycleScore = 0;
    for (size_t i = 2; i < playerHistory.size(); ++i) {
        int m0 = static_cast<int>(playerHistory[i - 2]);
        int m1 = static_cast<int>(playerHistory[i - 1]);
        int m2 = static_cast<int>(playerHistory[i]);
        if ((m0 + 1) % 3 == m1 && (m1 + 1) % 3 == m2) {
            cycleScore++;
        } else if ((m0 + 2) % 3 == m1 && (m1 + 2) % 3 == m2) {
            cycleScore--;
        }
    }

    int totalTransitions = transitionCounts[0] + transitionCounts[1] + transitionCounts[2];
    Move predictedPlayerMove;

    if (totalTransitions >= 2) {
        // Find most frequent transition
        int maxIdx = 0;
        int maxCount = transitionCounts[0];
        for (int i = 1; i < 3; ++i) {
            if (transitionCounts[i] > maxCount) {
                maxCount = transitionCounts[i];
                maxIdx = i;
            }
        }
        predictedPlayerMove = static_cast<Move>(maxIdx);
    } else if (std::abs(cycleScore) >= 2) {
        // Player is following a clockwise or counter-clockwise cycle
        int lastIdx = static_cast<int>(lastPlayerMove);
        if (cycleScore > 0) {
            predictedPlayerMove = static_cast<Move>((lastIdx + 1) % 3);
        } else {
            predictedPlayerMove = static_cast<Move>((lastIdx + 2) % 3);
        }
    } else {
        // General frequency count of player's moves
        std::array<int, 3> totalMoveCounts = {0, 0, 0};
        for (auto m : playerHistory) {
            int idx = static_cast<int>(m);
            if (idx >= 0 && idx < 3) totalMoveCounts[idx]++;
        }
        int maxIdx = 0;
        int maxCount = totalMoveCounts[0];
        for (int i = 1; i < 3; ++i) {
            if (totalMoveCounts[i] > maxCount) {
                maxCount = totalMoveCounts[i];
                maxIdx = i;
            }
        }
        predictedPlayerMove = static_cast<Move>(maxIdx);
    }

    // AI plays the counter to the predicted player move with 85% probability
    // (15% entropy to avoid infinite exploit loop if player plays meta-game)
    if (Random::getInstance().chance(0.85f)) {
        return getWinningMoveAgainst(predictedPlayerMove);
    } else {
        return getRandomMove();
    }
}

Move AIEngine::generateAIMove(
    Difficulty difficulty,
    const std::vector<Move>& playerHistory,
    const std::vector<Move>& aiHistory
) {
    switch (difficulty) {
        case Difficulty::Easy:
            return chooseEasy(playerHistory, aiHistory);
        case Difficulty::Normal:
            return chooseNormal(playerHistory, aiHistory);
        case Difficulty::Hard:
            return chooseHard(playerHistory, aiHistory);
        default:
            return chooseNormal(playerHistory, aiHistory);
    }
}

Move generateAIMove(
    Difficulty difficulty,
    const std::vector<Move>& playerHistory,
    const std::vector<Move>& aiHistory
) {
    return AIEngine::getInstance().generateAIMove(difficulty, playerHistory, aiHistory);
}

} // namespace rps::ai
