# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/aidanfollestad/Documents/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Rules
-keep class androidx.annotation.Keep {*;}
-keepclasseswithmembernames class * {
    @androidx.annotation.Keep *;
}

-dontwarn
-ignorewarnings

# RetroFit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Glide
-keep public class * implements com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# AndroidX
-keep,allowshrinking class androidx.** {*;}
-keep,allowshrinking interface androidx.** {*;}
-keep,allowshrinking,allowoptimization class com.google.android.material.** {*;}
-keep class android.support.v4.** {*;}

#
# jaudiotagger
#
-keepclassmembers class org.jaudiotagger.FileConstants {*;}
-keepclassmembers,allowoptimization class org.jaudiotagger.audio.** {<init>(...);public <methods>;public <fields>;}
-keepclassmembers,allowoptimization class org.jaudiotagger.utils.** {<init>(...);public <methods>;public <fields>;}
-keepclassmembers class org.jaudiotagger.tag.**{*;}
#


# Serialization
-keepnames class ** implements java.io.Serializable
-keep,allowoptimization class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Json
-keep class * {
    @com.google.gson.annotations.SerializedName *;
}


# StatusBarLyric API
-keep class StatusBarLyric.API.StatusBarLyric {*;}


#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.** {*;}

-keep class player.phonograph.preferences.** {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {*;}

-keep,allowoptimization class player.phonograph.ui.** {public <methods>;public <fields>;}
-keep,allowoptimization class player.phonograph.adapter.** {public <methods>;<fields>;}
-keep,allowoptimization class player.phonograph.dialogs.** {public <methods>;}
-keep,allowoptimization class player.phonograph.glide.** {<init>(...);public <methods>;}
-keep,allowoptimization class player.phonograph.service.** {public <methods>;public <fields>;}
-keep,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;public <fields>;<init>(...);}
-keep,allowoptimization,allowshrinking class player.phonograph.settings.** {public <methods>;}
-keepclassmembernames,allowoptimization class player.phonograph.notification.** {public <methods>;}
#-keep,allowshrinking class player.phonograph.* {public <methods>;}
