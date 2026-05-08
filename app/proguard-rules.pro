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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room: keep generated _Impl classes (instantiated via reflection)
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * implements androidx.room.DatabaseConfiguration { *; }
-keep class **_Impl { <init>(); }

# WorkManager: uses Room internally for WorkDatabase
-keep class androidx.work.impl.** { *; }

# Location callbacks used by NativeLocationProvider (F-Droid flavor, no Play Services)
-keep class * implements android.location.LocationListener { *; }
-keepclassmembers class * implements android.location.LocationListener { *; }
-keep class orinasa.njarasoa.maripanatokana.data.location.** { *; }