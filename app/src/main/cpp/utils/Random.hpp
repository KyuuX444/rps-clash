#pragma once

#include <random>

namespace rps::utils {

class Random {
public:
    static Random& getInstance();

    // Returns a random integer in [min, max] inclusive
    int nextInt(int min, int max);

    // Returns a random float in [0.0, 1.0)
    float nextFloat();

    // Returns true with given probability [0.0, 1.0]
    bool chance(float probability);

private:
    Random();
    std::mt19937 mRng;
};

} // namespace rps::utils
