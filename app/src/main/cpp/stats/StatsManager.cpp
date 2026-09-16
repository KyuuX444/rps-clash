#include "StatsManager.hpp"
#include "../storage/LocalStorage.hpp"
#include "../utils/Logger.hpp"
#include <sstream>
#include <iomanip>
#include <regex>

namespace rps::stats {

StatsManager& StatsManager::getInstance() {
    static StatsManager instance;
    return instance;
}

void StatsManager::init(const std::string& storageDir) {
    mFilePath = storageDir + "/game_stats.json";
    LOGI("Stats file path initialized: %s", mFilePath.c_str());

    if (storage::LocalStorage::fileExists(mFilePath)) {
        std::string content = storage::LocalStorage::readStringFromFile(mFilePath);
        if (!content.empty()) {
            fromJson(content);
        }
    }
}

void StatsManager::save() {
    if (mFilePath.empty()) return;
    std::string json = toJson();
    storage::LocalStorage::writeStringToFile(mFilePath, json);
    LOGI("Stats saved successfully.");
}

void StatsManager::recordMatch(
    bool isOnline,
    int wins,
    int losses,
    int draws,
    int rockUsed,
    int paperUsed,
    int scissorsUsed
) {
    ModeStats& s = isOnline ? mOnlineStats : mAiStats;
    s.totalMatches++;
    s.wins += wins;
    s.losses += losses;
    s.draws += draws;
    s.rockUsage += rockUsed;
    s.paperUsage += paperUsed;
    s.scissorsUsage += scissorsUsed;

    // Streak logic: if wins > losses, streak increases, else if losses > wins, streak resets
    if (wins > losses) {
        s.currentStreak++;
        if (s.currentStreak > s.bestStreak) {
            s.bestStreak = s.currentStreak;
        }
    } else if (losses > wins) {
        s.currentStreak = 0;
    }

    save();
}

void StatsManager::reset(bool isOnline) {
    ModeStats& s = isOnline ? mOnlineStats : mAiStats;
    s = ModeStats{};
    save();
}

void StatsManager::resetAll() {
    mAiStats = ModeStats{};
    mOnlineStats = ModeStats{};
    save();
}

static std::string formatModeJson(const ModeStats& s) {
    std::ostringstream ss;
    ss << "{\n"
       << "    \"totalMatches\": " << s.totalMatches << ",\n"
       << "    \"wins\": " << s.wins << ",\n"
       << "    \"losses\": " << s.losses << ",\n"
       << "    \"draws\": " << s.draws << ",\n"
       << "    \"currentStreak\": " << s.currentStreak << ",\n"
       << "    \"bestStreak\": " << s.bestStreak << ",\n"
       << "    \"rockUsage\": " << s.rockUsage << ",\n"
       << "    \"paperUsage\": " << s.paperUsage << ",\n"
       << "    \"scissorsUsage\": " << s.scissorsUsage << ",\n"
       << std::fixed << std::setprecision(1)
       << "    \"winRate\": " << s.getWinRate() << "\n"
       << "  }";
    return ss.str();
}

std::string StatsManager::toJson() const {
    std::ostringstream ss;
    ss << "{\n"
       << "  \"ai\": " << formatModeJson(mAiStats) << ",\n"
       << "  \"online\": " << formatModeJson(mOnlineStats) << "\n"
       << "}\n";
    return ss.str();
}

static int parseJsonInt(const std::string& section, const std::string& key) {
    std::regex re("\"" + key + "\"\\s*:\\s*(-?\\d+)");
    std::smatch match;
    if (std::regex_search(section, match, re)) {
        try {
            return std::stoi(match[1].str());
        } catch (...) {
            return 0;
        }
    }
    return 0;
}

static void parseModeJson(const std::string& section, ModeStats& outStats) {
    outStats.totalMatches = parseJsonInt(section, "totalMatches");
    outStats.wins = parseJsonInt(section, "wins");
    outStats.losses = parseJsonInt(section, "losses");
    outStats.draws = parseJsonInt(section, "draws");
    outStats.currentStreak = parseJsonInt(section, "currentStreak");
    outStats.bestStreak = parseJsonInt(section, "bestStreak");
    outStats.rockUsage = parseJsonInt(section, "rockUsage");
    outStats.paperUsage = parseJsonInt(section, "paperUsage");
    outStats.scissorsUsage = parseJsonInt(section, "scissorsUsage");
}

void StatsManager::fromJson(const std::string& jsonStr) {
    // Extract "ai" and "online" sections
    size_t aiPos = jsonStr.find("\"ai\"");
    size_t onlinePos = jsonStr.find("\"online\"");

    if (aiPos != std::string::npos) {
        size_t start = jsonStr.find('{', aiPos);
        size_t end = (onlinePos != std::string::npos && onlinePos > aiPos) 
                     ? onlinePos 
                     : jsonStr.find('}', start);
        if (start != std::string::npos && end != std::string::npos) {
            parseModeJson(jsonStr.substr(start, end - start), mAiStats);
        }
    }

    if (onlinePos != std::string::npos) {
        size_t start = jsonStr.find('{', onlinePos);
        size_t end = jsonStr.find('}', start);
        if (start != std::string::npos && end != std::string::npos) {
            parseModeJson(jsonStr.substr(start, end - start + 1), mOnlineStats);
        }
    }
}

} // namespace rps::stats
