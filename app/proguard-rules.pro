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
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

#AndroidX
-keep,allowshrinking class androidx.** {*;}
-keep,allowshrinking interface androidx.** {*;}
-keep,allowshrinking,allowoptimization class com.google.android.material.** {*;}
-keep class android.support.v4.** {*;}

#
# jaudiotagger
#
-keep,allowoptimization class org.jaudiotagger.** {*;}
#

-keepnames class ** implements java.io.Serializable
-keepnames class ** implements android.os.Parcelable



-keep class * {
    @com.google.gson.annotations.SerializedName *;
}

-keep class player.phonograph.preferences.** {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {*;}

-keep class StatusbarLyric.API.StatusBarLyric {*;}

-keep,allowoptimization class player.phonograph.adapter.** {<init>(...);public <methods>;<fields>;}
-keep,allowoptimization class player.phonograph.dialogs.** {public <methods>;}
-keep,allowoptimization class player.phonograph.glide.** {<init>(...);public <methods>;}
-keep,allowoptimization class player.phonograph.service.** {<init>(...);public <methods>;public <fields>;}
-keep class player.phonograph.ui.** {*;}
-keep,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;public <fields>;<init>(...);}
-keep,allowshrinking class player.phonograph.* {public <methods>;}
