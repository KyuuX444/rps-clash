#include "AudioEngine.hpp"
#include "../utils/Logger.hpp"

namespace rps::audio {

AudioEngine& AudioEngine::getInstance() {
    static AudioEngine instance;
    return instance;
}

AudioEngine::AudioEngine()
    : mMusicEnabled(true)
    , mSfxEnabled(true)
    , mMusicVolume(0.7f)
    , mSfxVolume(0.9f)
    , mWasMusicPlaying(false) {}

void AudioEngine::playSound(SoundEffect sfx) {
    if (!mSfxEnabled) return;
    if (mSoundCb) {
        mSoundCb(sfx);
    }
}

void AudioEngine::setMusicPlaying(bool play) {
    if (!mMusicEnabled && play) return;
    mWasMusicPlaying = play;
    if (mMusicCb) {
        mMusicCb(play);
    }
}

void AudioEngine::setMusicEnabled(bool enabled) {
    mMusicEnabled = enabled;
    if (!enabled && mMusicCb) {
        mMusicCb(false);
    } else if (enabled && mWasMusicPlaying && mMusicCb) {
        mMusicCb(true);
    }
}

void AudioEngine::setSfxEnabled(bool enabled) {
    mSfxEnabled = enabled;
}

void AudioEngine::setMusicVolume(float volume) {
    mMusicVolume = (volume < 0.0f) ? 0.0f : (volume > 1.0f ? 1.0f : volume);
}

void AudioEngine::setSfxVolume(float volume) {
    mSfxVolume = (volume < 0.0f) ? 0.0f : (volume > 1.0f ? 1.0f : volume);
}

void AudioEngine::onPause() {
    if (mMusicEnabled && mWasMusicPlaying && mMusicCb) {
        mMusicCb(false);
    }
}

void AudioEngine::onResume() {
    if (mMusicEnabled && mWasMusicPlaying && mMusicCb) {
        mMusicCb(true);
    }
}

} // namespace rps::audio
