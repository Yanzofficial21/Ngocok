# Prevent obfuscation for important classes
-keep class com.hpcontrol.** { *; }
-keep class com.hpcontrol.TelegramClient { *; }
-keep class com.hpcontrol.LockService { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}