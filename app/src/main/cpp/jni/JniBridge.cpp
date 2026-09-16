#include "JniHelpers.hpp"
#include "../game/GameCore.hpp"
#include "../ai/AIEngine.hpp"
#include "../stats/StatsManager.hpp"
#include "../audio/AudioEngine.hpp"
#include "../haptic/HapticBridge.hpp"
#include "../utils/Logger.hpp"

#include <vector>

using namespace rps::game;
using namespace rps::ai;
using namespace rps::stats;
using namespace rps::audio;
using namespace rps::haptic;
using namespace rps::jni;

static MatchManager gVsAiMatch;

extern "C" {

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_initCore(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring storageDir
) {
    std::string path = jstringToString(env, storageDir);
    StatsManager::getInstance().init(path);
    LOGI("Native Core initialized with path: %s", path.c_str());
}

JNIEXPORT jint JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_calculateResult(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint playerMove,
    jint opponentMove
) {
    Move p = static_cast<Move>(playerMove);
    Move o = static_cast<Move>(opponentMove);
    Result res = calculateResult(p, o);
    return static_cast<jint>(res);
}

JNIEXPORT jint JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_generateAIMove(
    JNIEnv* env,
    jobject /*thiz*/,
    jint difficulty,
    jintArray playerHistory,
    jintArray aiHistory
) {
    std::vector<Move> pHist;
    std::vector<Move> aHist;

    if (playerHistory) {
        jsize len = env->GetArrayLength(playerHistory);
        jint* elements = env->GetIntArrayElements(playerHistory, nullptr);
        for (jsize i = 0; i < len; ++i) {
            pHist.push_back(static_cast<Move>(elements[i]));
        }
        env->ReleaseIntArrayElements(playerHistory, elements, JNI_ABORT);
    }

    if (aiHistory) {
        jsize len = env->GetArrayLength(aiHistory);
        jint* elements = env->GetIntArrayElements(aiHistory, nullptr);
        for (jsize i = 0; i < len; ++i) {
            aHist.push_back(static_cast<Move>(elements[i]));
        }
        env->ReleaseIntArrayElements(aiHistory, elements, JNI_ABORT);
    }

    Difficulty diff = intToDifficulty(difficulty);
    Move aiMove = generateAIMove(diff, pHist, aHist);
    return static_cast<jint>(aiMove);
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_startNewVsAiMatch(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint bestOf
) {
    gVsAiMatch.reset(bestOf);
    LOGI("New VS AI match started: Best of %d", bestOf);
}

JNIEXPORT jintArray JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_playVsAiRound(
    JNIEnv* env,
    jobject /*thiz*/,
    jint playerMoveVal,
    jint difficultyVal
) {
    Move playerMove = static_cast<Move>(playerMoveVal);
    Difficulty diff = intToDifficulty(difficultyVal);

    std::vector<Move> pHist = gVsAiMatch.getPlayerMoveHistory();
    std::vector<Move> aHist = gVsAiMatch.getOpponentMoveHistory();

    Move aiMove = generateAIMove(diff, pHist, aHist);
    RoundRecord record = gVsAiMatch.playRound(playerMove, aiMove);

    jintArray resultArr = env->NewIntArray(6);
    jint buffer[6] = {
        static_cast<jint>(aiMove),
        static_cast<jint>(record.result),
        gVsAiMatch.getPlayerScore(),
        gVsAiMatch.getOpponentScore(),
        gVsAiMatch.isMatchOver() ? 1 : 0,
        gVsAiMatch.getRoundNumber()
    };
    env->SetIntArrayRegion(resultArr, 0, 6, buffer);
    return resultArr;
}

JNIEXPORT jstring JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_getStatsJson(
    JNIEnv* env,
    jobject /*thiz*/
) {
    std::string json = StatsManager::getInstance().toJson();
    return stringToJstring(env, json);
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_recordMatchResult(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean isOnline,
    jint wins,
    jint losses,
    jint draws,
    jint rock,
    jint paper,
    jint scissors
) {
    StatsManager::getInstance().recordMatch(
        isOnline, wins, losses, draws, rock, paper, scissors
    );
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_resetStats(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean isOnline
) {
    StatsManager::getInstance().reset(isOnline);
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_resetAllStats(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    StatsManager::getInstance().resetAll();
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_setAudioSettings(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean musicEnabled,
    jboolean sfxEnabled,
    jfloat musicVol,
    jfloat sfxVol
) {
    AudioEngine::getInstance().setMusicEnabled(musicEnabled);
    AudioEngine::getInstance().setSfxEnabled(sfxEnabled);
    AudioEngine::getInstance().setMusicVolume(musicVol);
    AudioEngine::getInstance().setSfxVolume(sfxVol);
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_setHapticSettings(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled,
    jfloat intensity
) {
    HapticBridge::getInstance().setEnabled(enabled);
    HapticBridge::getInstance().setIntensity(intensity);
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_triggerHaptic(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint pattern
) {
    HapticBridge::getInstance().trigger(static_cast<HapticPattern>(pattern));
}

JNIEXPORT void JNICALL
Java_com_kyuu_rpsclash_nativebridge_NativeBridge_playSound(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint sfx
) {
    AudioEngine::getInstance().playSound(static_cast<SoundEffect>(sfx));
}

} // extern "C"
