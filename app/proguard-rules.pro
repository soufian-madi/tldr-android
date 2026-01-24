# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Gson data classes used for API communication
-keep class com.example.tldr_ai.data.api.ClaudeRequest { *; }
-keep class com.example.tldr_ai.data.api.Message { *; }
-keep class com.example.tldr_ai.data.api.ClaudeResponse { *; }
-keep class com.example.tldr_ai.data.api.ContentBlock { *; }
-keep class com.example.tldr_ai.data.api.ClaudeSummaryResponse { *; }
-keep class com.example.tldr_ai.data.model.SummaryResult { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*

# Gson specific rules
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
