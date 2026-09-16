#pragma once

#include <jni.h>
#include <string>

namespace rps::jni {

inline std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* utfChars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(utfChars);
    env->ReleaseStringUTFChars(jstr, utfChars);
    return str;
}

inline jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

} // namespace rps::jni
