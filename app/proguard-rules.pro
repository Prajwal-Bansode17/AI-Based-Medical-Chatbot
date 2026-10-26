# =======================================================================
# MEDASSIST AI - PROGUARD / R8 RELEASE OPTIMIZATION RULES
# =======================================================================

# 1. Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# 2. Gson Serialization & Data Models
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Keep all project API models and data classes to prevent field stripping/obfuscation
-keep class com.example.ai_based_medical_chatbot.data.api.** { *; }
-keep class com.example.ai_based_medical_chatbot.ChatMemoryItem { *; }
-keep class com.example.ai_based_medical_chatbot.MemoryCategory { *; }
-keep class com.example.ai_based_medical_chatbot.ChatMessage { *; }
-keep class data.** { *; }
-keep class model.** { *; }

# 3. Kotlin Coroutines & Reflection
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# 4. MLKit Text Recognition & Translation
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 5. Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
