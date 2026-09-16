#pragma once

#include <string>

namespace rps::storage {

class LocalStorage {
public:
    static bool writeStringToFile(const std::string& path, const std::string& content);
    static std::string readStringFromFile(const std::string& path);
    static bool fileExists(const std::string& path);
};

} // namespace rps::storage
