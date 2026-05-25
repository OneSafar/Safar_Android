-keep class com.safar.app.data.remote.dto.** { *; }
-keep class com.safar.app.data.remote.model.** { *; }

// Gson uses reflection + field names. R8 minification can break JSON parsing for
// planner models and request/response bodies in release builds (symptom: empty
// plan IDs/subjects, "Unknown error", and misleading 404s during imports).
-keep class com.safar.app.domain.model.studyplanner.** { *; }
-keep class com.safar.app.data.remote.api.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }