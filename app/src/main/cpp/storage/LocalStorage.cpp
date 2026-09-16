#include "LocalStorage.hpp"
#include "../utils/Logger.hpp"
#include <fstream>
#include <sstream>
#include <sys/stat.h>

namespace rps::storage {

bool LocalStorage::writeStringToFile(const std::string& path, const std::string& content) {
    std::ofstream ofs(path, std::ios::out | std::ios::trunc);
    if (!ofs.is_open()) {
        LOGE("Failed to open file for writing: %s", path.c_str());
        return false;
    }
    ofs << content;
    ofs.flush();
    ofs.close();
    return true;
}

std::string LocalStorage::readStringFromFile(const std::string& path) {
    std::ifstream ifs(path);
    if (!ifs.is_open()) {
        LOGW("Failed to open file for reading (may not exist yet): %s", path.c_str());
        return "";
    }
    std::stringstream buffer;
    buffer << ifs.rdbuf();
    return buffer.str();
}

bool LocalStorage::fileExists(const std::string& path) {
    struct stat buffer;
    return (stat(path.c_str(), &buffer) == 0);
}

} // namespace rps::storage
