#include "HapticBridge.hpp"

namespace rps::haptic {

HapticBridge& HapticBridge::getInstance() {
    static HapticBridge instance;
    return instance;
}

HapticBridge::HapticBridge()
    : mEnabled(true)
    , mIntensity(0.5f) {}

void HapticBridge::trigger(HapticPattern pattern) {
    if (!mEnabled) return;
    if (mCallback) {
        mCallback(pattern);
    }
}

} // namespace rps::haptic
