# ProGuard rules for CloudVault

# Room Database
-keep class com.shamil.cloudvault.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Data classes
-keepclassmembers class com.shamil.cloudvault.model.** {
    <init>(...);
    public ** get*();
    public void set*(***);
}

-keepclassmembers class com.shamil.cloudvault.domain.model.** {
    <init>(...);
    public ** get*();
    public void set*(***);
}

-keepclassmembers class com.shamil.cloudvault.data.local.** {
    <init>(...);
    public ** get*();
    public void set*(***);
}

# Coil
-keep class coil.** { *; }
-keepclassmembers class * extends coil.intercept.Interceptor {
    *** intercept(...);
}

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Kotlin Coroutines
-keepclasseswithmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Serialization
-keepclassmembers class kotlin.** {
    volatile <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep exceptions
-keep public class * extends java.lang.Exception

# Optimize
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Verbose
-verbose

