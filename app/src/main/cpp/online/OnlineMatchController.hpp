#pragma once

#include <string>
#include <functional>
#include "../game/Move.hpp"
#include "../game/Result.hpp"

namespace rps::online {

enum class OnlineState {
    Disconnected,
    Connecting,
    Lobby,
    InQueue,
    InRoomWaiting,
    RoundActive,
    RoundResult,
    MatchOver
};

struct OnlineRoundData {
    int round;
    game::Move playerMove;
    game::Move opponentMove;
    game::Result result;
    int playerScore;
    int opponentScore;
};

class OnlineMatchController {
public:
    static OnlineMatchController& getInstance();

    void reset();

    void setState(OnlineState state) { mState = state; }
    OnlineState getState() const { return mState; }

    void setRoomCode(const std::string& code) { mRoomCode = code; }
    const std::string& getRoomCode() const { return mRoomCode; }

    void setSessionId(const std::string& session) { mSessionId = session; }
    const std::string& getSessionId() const { return mSessionId; }

    void setPlayerName(const std::string& name) { mPlayerName = name; }
    const std::string& getPlayerName() const { return mPlayerName; }

    void setOpponentName(const std::string& name) { mOpponentName = name; }
    const std::string& getOpponentName() const { return mOpponentName; }

    void setScores(int myScore, int oppScore) {
        mPlayerScore = myScore;
        mOpponentScore = oppScore;
    }
    int getPlayerScore() const { return mPlayerScore; }
    int getOpponentScore() const { return mOpponentScore; }

    void setTargetWins(int wins) { mTargetWins = wins; }
    int getTargetWins() const { return mTargetWins; }

    void setCurrentRound(int round) { mCurrentRound = round; }
    int getCurrentRound() const { return mCurrentRound; }

private:
    OnlineMatchController();

    OnlineState mState;
    std::string mRoomCode;
    std::string mSessionId;
    std::string mPlayerName;
    std::string mOpponentName;
    int mPlayerScore;
    int mOpponentScore;
    int mTargetWins;
    int mCurrentRound;
};

} // namespace rps::online
