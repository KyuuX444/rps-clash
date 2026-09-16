#pragma once

#include <string>

namespace rps::ai {

enum class Difficulty {
    Easy = 0,
    Normal = 1,
    Hard = 2
};

inline std::string difficultyToString(Difficulty diff) {
    switch (diff) {
        case Difficulty::Easy: return "Easy";
        case Difficulty::Normal: return "Normal";
        case Difficulty::Hard: return "Hard";
        default: return "Normal";
    }
}

inline Difficulty intToDifficulty(int val) {
    switch (val) {
        case 0: return Difficulty::Easy;
        case 1: return Difficulty::Normal;
        case 2: return Difficulty::Hard;
        default: return Difficulty::Normal;
    }
}

} // namespace rps::ai
