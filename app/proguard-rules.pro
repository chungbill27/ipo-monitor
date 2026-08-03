# IPO Monitor ProGuard Rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-keep class com.ipomonitor.data.model.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gemini AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
