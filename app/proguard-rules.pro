# ProGuard rules for Recordly
# ----- Defaults -----
# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep the entry point
-keep class com.recordly.app.RecordlyApplication { *; }
-keep class com.recordly.app.MainActivity { *; }

# Keep the RecordingService since it's referenced in the manifest
-keep class com.recordly.app.service.RecordingService { *; }

# Keep data classes used with DataStore serialization
-keep class com.recordly.app.data.UserPreferences { *; }
-keep class com.recordly.app.data.Recording { *; }

# Keep Compose-related classes
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Coil
-dontwarn coil.**
