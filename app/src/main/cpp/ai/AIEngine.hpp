#pragma once

#include "Difficulty.hpp"
#include "../game/Move.hpp"
#include <vector>
#include <array>

namespace rps::ai {

class AIEngine {
public:
    static AIEngine& getInstance();

    game::Move generateAIMove(
        Difficulty difficulty,
        const std::vector<game::Move>& playerHistory,
        const std::vector<game::Move>& aiHistory
    );

private:
    AIEngine() = default;

    game::Move chooseEasy(
        const std::vector<game::Move>& playerHistory,
        const std::vector<game::Move>& aiHistory
    );

    game::Move chooseNormal(
        const std::vector<game::Move>& playerHistory,
        const std::vector<game::Move>& aiHistory
    );

    game::Move chooseHard(
        const std::vector<game::Move>& playerHistory,
        const std::vector<game::Move>& aiHistory
    );

    game::Move getRandomMove();
};

// Pure function as requested by the user
game::Move generateAIMove(
    Difficulty difficulty,
    const std::vector<game::Move>& playerHistory = {},
    const std::vector<game::Move>& aiHistory = {}
);

} // namespace rps::ai
