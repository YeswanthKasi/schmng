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
# Prevent obfuscation of the Person model used in Firestore
# Prevent obfuscation of all models in your model package
-keep class com.ecorvi.schmng.ui.data.model.** { *; }
-keep class com.ecorvi.schmng.models.** { *; }

# Keep Firestore annotations and metadata
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep Firestore serialization info
-keepclassmembers class com.ecorvi.schmng.models.** {
    *;
}

# Specifically keep the AttendanceRecord class and its fields
-keepclassmembers class com.ecorvi.schmng.models.AttendanceRecord {
    <init>();
    <fields>;
}

# Keep enum classes
-keepclassmembers enum * { *; }

# Keep Firestore-specific classes
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.database.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Keep kotlin.Metadata annotations
-keep class kotlin.Metadata { *; }
