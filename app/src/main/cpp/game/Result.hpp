#pragma once

#include <string>

namespace rps::game {

enum class Result {
    Win = 0,
    Lose = 1,
    Draw = 2,
    None = -1
};

inline std::string resultToString(Result res) {
    switch (res) {
        case Result::Win: return "Win";
        case Result::Lose: return "Lose";
        case Result::Draw: return "Draw";
        default: return "None";
    }
}

} // namespace rps::game
