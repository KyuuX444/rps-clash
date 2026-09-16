#pragma once

#include <string>

namespace rps::game {

enum class Move {
    Rock = 0,
    Paper = 1,
    Scissors = 2,
    None = -1
};

inline std::string moveToString(Move move) {
    switch (move) {
        case Move::Rock: return "Rock";
        case Move::Paper: return "Paper";
        case Move::Scissors: return "Scissors";
        default: return "None";
    }
}

inline Move stringToMove(const std::string& str) {
    if (str == "Rock" || str == "rock") return Move::Rock;
    if (str == "Paper" || str == "paper") return Move::Paper;
    if (str == "Scissors" || str == "scissors") return Move::Scissors;
    return Move::None;
}

inline Move getWinningMoveAgainst(Move move) {
    switch (move) {
        case Move::Rock: return Move::Paper;
        case Move::Paper: return Move::Scissors;
        case Move::Scissors: return Move::Rock;
        default: return Move::Rock;
    }
}

inline Move getLosingMoveAgainst(Move move) {
    switch (move) {
        case Move::Rock: return Move::Scissors;
        case Move::Paper: return Move::Rock;
        case Move::Scissors: return Move::Paper;
        default: return Move::Scissors;
    }
}

} // namespace rps::game
