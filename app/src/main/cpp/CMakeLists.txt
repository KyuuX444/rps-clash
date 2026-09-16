cmake_minimum_required(VERSION 3.22.1)

project("gamecore")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(
    .
    game
    ai
    stats
    storage
    audio
    haptic
    online
    network
    utils
    jni
)

add_library(
    gamecore
    SHARED
    game/GameCore.cpp
    ai/AIEngine.cpp
    stats/StatsManager.cpp
    storage/LocalStorage.cpp
    audio/AudioEngine.cpp
    haptic/HapticBridge.cpp
    online/OnlineMatchController.cpp
    network/NetworkProtocol.cpp
    utils/Random.cpp
    jni/JniBridge.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    gamecore
    ${log-lib}
)
