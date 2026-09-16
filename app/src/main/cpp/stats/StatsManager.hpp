#pragma once

#include <string>

namespace rps::stats {

struct ModeStats {
    int totalMatches = 0;
    int wins = 0;
    int losses = 0;
    int draws = 0;
    int currentStreak = 0;
    int bestStreak = 0;
    int rockUsage = 0;
    int paperUsage = 0;
    int scissorsUsage = 0;

    float getWinRate() const {
        if (totalMatches <= 0) return 0.0f;
        return (static_cast<float>(wins) / static_cast<float>(totalMatches)) * 100.0f;
    }
};

class StatsManager {
public:
    static StatsManager& getInstance();

    void init(const std::string& storageDir);
    void save();

    void recordMatch(bool isOnline, int wins, int losses, int draws, int rockUsed, int paperUsed, int scissorsUsed);
    void reset(bool isOnline);
    void resetAll();

    const ModeStats& getAiStats() const { return mAiStats; }
    const ModeStats& getOnlineStats() const { return mOnlineStats; }

    std::string toJson() const;
    void fromJson(const std::string& jsonStr);

private:
    StatsManager() = default;

    std::string mFilePath;
    ModeStats mAiStats;
    ModeStats mOnlineStats;
};

} // namespace rps::stats
