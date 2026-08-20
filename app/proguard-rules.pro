# ==============================================================================
# SAFAR R8 / ProGuard Keep Rules
# ==============================================================================

# Data Transfer Objects, APIs, and Data Layer Models
-keep class com.safarparmar.app.data.remote.dto.** { *; }
-keep class com.safarparmar.app.data.remote.model.** { *; }
-keep class com.safarparmar.app.data.remote.api.** { *; }
-keep class com.safarparmar.app.domain.model.** { *; }
-keep class com.safarparmar.app.feature.**.data.local.** { *; }
-keep class com.safarparmar.app.feature.**.data.remote.** { *; }
-keep class com.safarparmar.app.feature.**.domain.model.** { *; }

# Gson serialization & annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Socket.IO & Engine.IO
-keep class io.socket.** { *; }
-keep class io.socket.engineio.client.** { *; }

# Razorpay Payment SDK
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-dontwarn proguard.annotation.Keep
-dontwarn proguard.annotation.KeepClassMembers