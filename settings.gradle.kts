dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://jitpack.io")
    }
    versionCatalogs {
        create("depsLibs") {

            //
            // Common Androidx/Jetpack
            //
            val versionActivity = "activity"
            version(versionActivity, "1.6.0")

            library("androidx.core",
                    "androidx.core:core-ktx:1.9.0")
            library("androidx.appcompat",
                    "androidx.appcompat:appcompat:1.5.1")
            library("androidx.activity",
                    "androidx.activity",
                    "activity-ktx").versionRef(versionActivity)
            library("androidx.fragment",
                    "androidx.fragment:fragment-ktx:1.5.3")
            library("androidx.lifecycle_runtime",
                    "androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

            library("androidx.annotation",
                    "androidx.annotation:annotation:1.5.0")
            library("androidx.preference",
                    "androidx.preference:preference-ktx:1.2.0")

            library("androidx.recyclerview",
                    "androidx.recyclerview:recyclerview:1.2.1")

            library("androidx.constraintlayout",
                    "androidx.constraintlayout:constraintlayout:2.1.4")
            library("androidx.percentlayout",
                    "androidx.percentlayout:percentlayout:1.0.0")
            library("androidx.swiperefreshlayout",
                    "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

            library("androidx.media",
                    "androidx.media:media:1.6.0")
            library("androidx.cardview",
                    "androidx.cardview:cardview:1.0.0")
            library("androidx.palette",
                    "androidx.palette:palette-ktx:1.0.0")

            library("google.material",
                    "com.google.android.material:material:1.6.1")

            //
            // Jetpack Compose
            //
            val versionCompose = "compose"
            val versionComposeCompiler = "composeCompiler"
            version(versionCompose, "1.3.1")
            version(versionComposeCompiler, "1.3.1")

            library("compose.runtime",
                    "androidx.compose.runtime",
                    "runtime").versionRef(versionCompose)
            library("compose.foundation",
                    "androidx.compose.foundation",
                    "foundation").versionRef(versionCompose)
            library("compose.ui",
                    "androidx.compose.ui",
                    "ui").versionRef(versionCompose)
            library("compose.ui_tooling",
                    "androidx.compose.ui",
                    "ui-tooling").versionRef(versionCompose)
            library("compose.material",
                    "androidx.compose.material",
                    "material").versionRef(versionCompose)
            library("compose.activity",
                    "androidx.activity",
                    "activity-compose").versionRef(versionActivity)

            bundle("compose",
                   listOf(
                       "compose.runtime",
                       "compose.foundation",
                       "compose.ui",
                       "compose.material",
                       "compose.activity",
                   )
            )

            //
            // popular
            //
            library("okhttp3",
                    "com.squareup.okhttp3:okhttp:4.10.0")
            library("retrofit2",
                    "com.squareup.retrofit2:retrofit:2.9.0")
            library("kotlinx.serialization.json",
                    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
            library("coil",
                    "io.coil-kt:coil:2.2.2")

            //
            // other
            //
            val versionMaterialDialogs = "material_dialogs"
            version(versionMaterialDialogs, "3.3.0")

            library("materialDialogs.core",
                    "com.afollestad.material-dialogs",
                    "core").versionRef(versionMaterialDialogs)
            library("materialDialogs.input",
                    "com.afollestad.material-dialogs",
                    "input").versionRef(versionMaterialDialogs)
            library("materialDialogs.color",
                    "com.afollestad.material-dialogs",
                    "color").versionRef(versionMaterialDialogs)

            bundle("materialDialogs",
                   listOf(
                       "materialDialogs.core",
                       "materialDialogs.input",
                       "materialDialogs.color"
                   )
            )

            library("composeMaterialDialogs",
                    "io.github.vanpra.compose-material-dialogs:core:0.9.0")


            val versionMaterialTool = "material-tools"
            version(versionMaterialTool, "0.0.9")
            library("mt.colorRes",
                    "com.github.chr56.material-tools",
                    "mdColorRes").versionRef(versionMaterialTool)
            library("mt.util",
                    "com.github.chr56.material-tools",
                    "mdUtil").versionRef(versionMaterialTool)
            library("mt.pref",
                    "com.github.chr56.material-tools",
                    "mdPref").versionRef(versionMaterialTool)
            library("mt.tint",
                    "com.github.chr56.material-tools",
                    "mdTint").versionRef(versionMaterialTool)

            bundle("materialTools",
                   listOf("mt.colorRes", "mt.util", "mt.pref", "mt.tint")
            )

            library("menuDsl",
                    "io.github.chr56:android-menu-dsl:0.1.0")
            library("seekArc",
                    "com.github.chr56:SeekArc:1.0")
            library("slidingUpPanel",
                    "com.github.kabouzeid:AndroidSlidingUpPanel:6")
            library("recyclerviewFastscroll",
                    "com.simplecityapps:recyclerview-fastscroll:2.0.1")
            library("licensesdialog",
                    "de.psdev.licensesdialog:licensesdialog:2.1.0")
            library("jaudiotagger",
                    "com.github.chr56:jaudiotagger:0.0.1-no-mp4")
            library("observablescrollview",
                    "com.github.chr56:Android-ObservableScrollView-Retro:0.0.1")
            library("materialIntro",
                    "com.heinrichreimersoftware:material-intro:2.0.0")
            library("advrecyclerview",
                    "com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")
            library("composeReorderable",
                    "org.burnoutcrew.composereorderable:reorderable:0.9.6")
            library("statusBarLyricsApi",
                    "com.github.577fkj:StatusBarApiExample:v2.0")
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.buildFileName = "build.gradle.kts"

include(":app")
include(":tools:changelog-generator")
includeBuild(file("tools/release-tool"))
