#include "Random.hpp"
#include <chrono>

namespace rps::utils {

Random& Random::getInstance() {
    static Random instance;
    return instance;
}

Random::Random() {
    auto seed = static_cast<unsigned int>(
        std::chrono::high_resolution_clock::now().time_since_epoch().count()
    );
    mRng.seed(seed);
}

int Random::nextInt(int min, int max) {
    if (min >= max) return min;
    std::uniform_int_distribution<int> dist(min, max);
    return dist(mRng);
}

float Random::nextFloat() {
    std::uniform_real_distribution<float> dist(0.0f, 1.0f);
    return dist(mRng);
}

bool Random::chance(float probability) {
    if (probability <= 0.0f) return false;
    if (probability >= 1.0f) return true;
    return nextFloat() < probability;
}

} // namespace rps::utils
