# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# ============================================================================
# Socket.IO / Engine.IO / OkHttp — keep rules for R8
# ============================================================================

# Socket.IO client uses reflection for event handling
-keep class io.socket.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.engineio.** { *; }
-keep class io.socket.emitter.** { *; }
-keep class io.socket.parser.** { *; }

# OkHttp (transitive dependency of Socket.IO)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Engine.IO uses reflection for transport selection
-keepclassmembers class io.socket.engineio.client.Transport {
    <init>(...);
}
-keepclassmembers class io.socket.engineio.client.transports.** {
    <init>(...);
}

# Keep JSON parsing classes used by Socket.IO
-keep class org.json.** { *; }