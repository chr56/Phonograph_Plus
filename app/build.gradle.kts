@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationBaseFlavor
import tools.release.git.getGitHash
import tools.release.registerPublishTask
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("tools.release")
}

val isSigningFileExist: Boolean = rootProject.file("signing.properties").exists()
var signingProperties = Properties()
if (isSigningFileExist) {
    rootProject.file("signing.properties").inputStream().use {
        signingProperties.load(it)
    }
}

android {
    compileSdk = 33
    buildToolsVersion = "33.0.0"
    namespace = "player.phonograph"

    val appName = "Phonograph Plus"

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 33

        renderscriptTargetApi = 29
        vectorDrawables.useSupportLibrary = true

        applicationId = "player.phonograph.plus"
        versionCode = 1010
        versionName = "1.1.0"


        setProperty("archivesBaseName", "PhonographPlus_$versionName")

        proguardFiles(File("proguard-rules-base.pro"), File("proguard-rules-app.pro"))


        manifestPlaceholders["GIT_COMMIT_HASH"] = "-"
    }

    signingConfigs {
        create("release") {
            if (isSigningFileExist) {
                storeFile = File(signingProperties["storeFile"] as String)
                storePassword = signingProperties["storePassword"] as String
                keyAlias = signingProperties["keyAlias"] as String
                keyPassword = signingProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            // signing
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            // shrink
            isMinifyEnabled = true
            isShrinkResources = true

            // git tracker
            manifestPlaceholders["GIT_COMMIT_HASH"] = getGitHash(false)
        }
        getByName("debug") {
            // signing as well
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            // package name
            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensions += listOf("purpose")
    productFlavors {
        // Stable or LTS release
        create("stable") {
            dimension = "purpose"

            resValue("string", "app_name", appName)
        }
        // Preview release
        create("preview") {
            dimension = "purpose"
            matchingFallbacks.add("stable")

            resValue("string", "app_name", "$appName Preview")
            applicationIdSuffix = ".preview"
        }
        // for checkout to locate a bug and ci etc.
        create("checkout") {
            dimension = "purpose"
            matchingFallbacks.add("stable")

            resValue("string", "app_name", "$appName Checkout")
            applicationIdSuffix = ".checkout"


            manifestPlaceholders["GIT_COMMIT_HASH"] = getGitHash(false)
        }
    }
    androidComponents {
        beforeVariants(selector().withBuildType("release")) { variantBuilder ->
            val favors = variantBuilder.productFlavors
            // no "release" type
            if (favors.contains("purpose" to "checkout")) {
                variantBuilder.enable = false
            }
        }

        val name = appName.replace(Regex("\\s"), "") //remove white space
        onVariants(selector().withBuildType("release")) { variant ->
            val version = (android.defaultConfig as ApplicationBaseFlavor).versionName ?: "N/A"
            tasks.registerPublishTask(name, version, variant)
        }
    }

    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("InvalidPackage")

        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
        )
    }

}

/**
 * Now, this project is using [VersionCatalog] (./gradle/libs.versions.toml)
 */
dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.preference)

    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.percentlayout)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidx.media)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.palette)
    implementation(libs.bundles.androidx.datastore)

    implementation(libs.google.material)

    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.bundles.materialTools)

    implementation(libs.menuDsl)
    implementation(libs.seekArc)
    implementation(libs.slidingUpPanel)

    implementation(libs.bundles.materialDialogs)
    implementation(libs.composeMaterialDialogs)

    implementation(libs.okhttp3)
    implementation(libs.retrofit2)
    implementation(libs.coil)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.licensesdialog)
    implementation(libs.jaudiotagger)
    implementation(libs.observablescrollview)
    implementation(libs.appIntro)
    implementation(libs.advrecyclerview)
    implementation(libs.recyclerviewFastscroll)
    implementation(libs.composeReorderable)
    implementation(libs.bundles.composeSettings) {
        val uiTooling = libs.compose.ui.tooling.get().module
        exclude(group = uiTooling.group, module = uiTooling.name)
    }
    implementation(libs.statusBarLyricsApi)
}
