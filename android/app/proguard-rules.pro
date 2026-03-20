# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================================
# Stack Traces
# ============================================================================

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in stack traces
-renamesourcefileattribute SourceFile

# ============================================================================
# Remove Logging in Release Builds
# ============================================================================

# Strip debug and verbose log calls
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ============================================================================
# Kotlin
# ============================================================================

# Keep Kotlin metadata for reflection (minimal set needed)
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================================
# App Data Classes
# ============================================================================

# Keep data classes in the data package (for DataStore/serialization)
-keep class com.rockyriverapps.tabletorch.data.AppSettings { *; }
-keepclassmembers class com.rockyriverapps.tabletorch.data.AppSettings { *; }

# Keep ColorPalette and its companion for JSON serialization
# (defense-in-depth alongside the @Immutable class keep rule)
-keep class com.rockyriverapps.tabletorch.data.ColorPalette { *; }
-keepclassmembers class com.rockyriverapps.tabletorch.data.ColorPalette { *; }
-keep class com.rockyriverapps.tabletorch.data.ColorPalette$Companion { *; }

# Keep ParticleShape enum constant names (persisted via .name / valueOf())
-keepclassmembers class com.rockyriverapps.tabletorch.models.ParticleShape {
    <fields>;
}

# ============================================================================
# Android Lifecycle / ViewModel
# ============================================================================

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep ViewModelFactory classes
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory { *; }

# ============================================================================
# Navigation
# ============================================================================

# Keep Parcelable and Serializable for navigation arguments
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# ============================================================================
# Enums
# ============================================================================

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Generic Signatures and Annotations
# ============================================================================

# Keep generic signature for type parameters (needed for Kotlin generics)
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# ============================================================================
# Suppress Warnings
# ============================================================================

# Suppress warnings for missing classes that are optional
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ============================================================================
# Jetpack Compose (targeted rules — Compose compiler handles most of this)
# ============================================================================

# Keep stability annotations so Compose compiler can optimize recomposition
-keep @androidx.compose.runtime.Stable class * { *; }
-keep @androidx.compose.runtime.Immutable class * { *; }

# ============================================================================
# Navigation Compose
# ============================================================================

-keepnames class * extends androidx.navigation.Navigator

# ============================================================================
# DataStore Preferences
# ============================================================================

-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# ============================================================================
# App-Specific Classes
# ============================================================================

# Keep MainViewModel and Factory for reflection-based instantiation
-keep class com.rockyriverapps.tabletorch.MainViewModel { *; }
-keep class com.rockyriverapps.tabletorch.MainViewModel$Factory { *; }
