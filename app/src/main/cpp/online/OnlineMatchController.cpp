#include "OnlineMatchController.hpp"

namespace rps::online {

OnlineMatchController& OnlineMatchController::getInstance() {
    static OnlineMatchController instance;
    return instance;
}

OnlineMatchController::OnlineMatchController() {
    reset();
}

void OnlineMatchController::reset() {
    mState = OnlineState::Disconnected;
    mRoomCode.clear();
    mSessionId.clear();
    mPlayerName = "Player";
    mOpponentName = "Opponent";
    mPlayerScore = 0;
    mOpponentScore = 0;
    mTargetWins = 2; // Default best of 3
    mCurrentRound = 1;
}

} // namespace rps::online
