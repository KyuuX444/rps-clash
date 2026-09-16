#pragma once

#include <functional>

namespace rps::haptic {

enum class HapticPattern {
    ButtonPress = 0,
    MoveSelection = 1,
    Countdown = 2,
    Win = 3,
    Lose = 4,
    Draw = 5,
    MatchResult = 6
};

class HapticBridge {
public:
    using HapticCallback = std::function<void(HapticPattern)>;

    static HapticBridge& getInstance();

    void setCallback(HapticCallback cb) { mCallback = cb; }
    void trigger(HapticPattern pattern);

    void setEnabled(bool enabled) { mEnabled = enabled; }
    bool isEnabled() const { return mEnabled; }

    void setIntensity(float intensity) { mIntensity = intensity; }
    float getIntensity() const { return mIntensity; }

private:
    HapticBridge();

    bool mEnabled = true;
    float mIntensity = 0.5f; // Light-medium default
    HapticCallback mCallback;
};

} // namespace rps::haptic
