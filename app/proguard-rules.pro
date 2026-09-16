# Add project specific ProGuard rules here.
-keep class com.kyuu.rpsclash.nativebridge.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.java_websocket.** { *; }
-keep class com.google.gson.** { *; }
