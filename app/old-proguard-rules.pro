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
-keep @androidx.annotation.Keep class * {*;}
-keep class androidx.annotation.Keep
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}



# OkHttp3
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# RetroFit
#-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
@retrofit2.http.* <methods>;
}
# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation


# Glide
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...);}
-keep class * implements com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

#  Gson
# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }
# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken


# AndroidX
-keep,allowshrinking class androidx.** {*;}
-keep,allowshrinking interface androidx.** {*;}
-keep,allowshrinking,allowoptimization class com.google.android.material.** {*;}
-keep class android.support.v4.** {*;}


# jaudiotagger
-keepclassmembers,allowoptimization class org.jaudiotagger.audio.** {<init>(...);public <methods>;public <fields>;}
-keepclassmembers class org.jaudiotagger.tag.**{*;}
#


# Serialization
#-keepnames class ** implements java.io.Serializable

# StatusBarLyric API
-keep class StatusBarLyric.API.StatusBarLyric {*;}


# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
# kotlin coroutines
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}

#
#  Phonograph
#

-keep class lib.phonograph.view.** {*;}
-keep class lib.phonograph.preference.** {*;}

-keep class player.phonograph.preferences.** {*;}
-keep class player.phonograph.views.** {*;}
-keep class player.phonograph.model.** {*;}

-keep,allowoptimization,allowshrinking class player.phonograph.ui.** {public <methods>;public <fields>;}
-keep,allowoptimization,allowshrinking class player.phonograph.service.** {public <methods>;public <fields>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.adapter.** {public <methods>;<fields>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.dialogs.** {public <methods>;}
-keepnames,allowoptimization,allowshrinking class player.phonograph.util.** {public <methods>;public <fields>;<init>(...);}
-keepnames,allowoptimization,allowshrinking class player.phonograph.glide.** {<init>(...);public <methods>;}
-keepclassmembernames,allowoptimization,allowshrinking class player.phonograph.settings.** {public <methods>;}
-keepclassmembernames,allowoptimization class player.phonograph.notification.** {public <methods>;}
