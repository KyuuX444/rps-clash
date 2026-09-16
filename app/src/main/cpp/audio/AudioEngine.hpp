#pragma once

#include <functional>

namespace rps::audio {

enum class SoundEffect {
    Tap = 0,
    Select = 1,
    Countdown = 2,
    MatchStart = 3,
    Win = 4,
    Lose = 5,
    Draw = 6,
    MatchEnd = 7
};

class AudioEngine {
public:
    using SoundCallback = std::function<void(SoundEffect)>;
    using MusicCallback = std::function<void(bool play)>;

    static AudioEngine& getInstance();

    void setSoundCallback(SoundCallback cb) { mSoundCb = cb; }
    void setMusicCallback(MusicCallback cb) { mMusicCb = cb; }

    void playSound(SoundEffect sfx);
    void setMusicPlaying(bool play);

    void setMusicEnabled(bool enabled);
    bool isMusicEnabled() const { return mMusicEnabled; }

    void setSfxEnabled(bool enabled);
    bool isSfxEnabled() const { return mSfxEnabled; }

    void setMusicVolume(float volume);
    float getMusicVolume() const { return mMusicVolume; }

    void setSfxVolume(float volume);
    float getSfxVolume() const { return mSfxVolume; }

    void onPause();
    void onResume();

private:
    AudioEngine();

    bool mMusicEnabled = true;
    bool mSfxEnabled = true;
    float mMusicVolume = 0.7f;
    float mSfxVolume = 0.9f;
    bool mWasMusicPlaying = false;

    SoundCallback mSoundCb;
    MusicCallback mMusicCb;
};

} // namespace rps::audio
