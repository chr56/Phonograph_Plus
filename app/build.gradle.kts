@file:Suppress("UnstableApiUsage")

import tools.release.git.getGitHash
import tools.release.registerPublishTask
import tools.release.text.NameSegment
import java.util.Properties

plugins {
    alias(libs.plugins.androidGradlePlugin)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.artifactsRelease)
}

val isSigningFileExist: Boolean = rootProject.file("signing.properties").exists()
var signingProperties = Properties()
if (isSigningFileExist) {
    rootProject.file("signing.properties").inputStream().use {
        signingProperties.load(it)
    }
}

android {
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    namespace = "player.phonograph"

    val appName = "Phonograph Plus"

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        applicationId = "player.phonograph.plus"
        versionCode = 1085
        versionName = "1.9-dev0"


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

            // git revision tracker
            manifestPlaceholders["GIT_COMMIT_HASH"] = getGitHash(false)
            vcsInfo.include = false // we have our means
        }
        getByName("debug") {
            // signing as well
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            // package name
            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensions += listOf("target", "channel")
    productFlavors {
        // Stable or LTS release
        create("stable") {
            dimension = "channel"

            resValue("string", "app_name", appName)
        }
        // Preview release
        create("preview") {
            dimension = "channel"
            matchingFallbacks.add("stable")

            resValue("string", "app_name", "$appName Preview")
            applicationIdSuffix = ".preview"

            isDefault = true
        }
        // for checkout to locate a bug and ci etc.
        create("checkout") {
            dimension = "channel"
            matchingFallbacks.add("stable")

            resValue("string", "app_name", "$appName Checkout")
            applicationIdSuffix = ".checkout"


            manifestPlaceholders["GIT_COMMIT_HASH"] = getGitHash(false)
        }

        create("modern") {
            dimension = "target"

            isDefault = true
        }
        create("legacy") {
            dimension = "target"
            matchingFallbacks.add("modern")

            targetSdk = 28
        }

    }
    androidComponents {

        val moduleName = project.name
        onVariants(selector().all()) { variant ->
            // Rename
            for (output in variant.outputs) {
                val outputImpl = output as? com.android.build.api.variant.impl.VariantOutputImpl ?: continue
                val origin = outputImpl.outputFileName.get()
                val new = origin.replace(moduleName, "PhonographPlus-${output.versionName.get()}")
                outputImpl.outputFileName.set(new)
            }
        }

        beforeVariants(selector().withBuildType("release")) { variantBuilder ->
            val favors = variantBuilder.productFlavors
            // no "release" type
            if (favors.contains("channel" to "checkout")) {
                variantBuilder.enable = false
            }
        }

        val name = appName.replace(Regex("\\s"), "") //remove white space
        onVariants(selector().withBuildType("release")) { variant ->
            tasks.registerPublishTask(name, variant)
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

    kotlinOptions {
        jvmTarget = "17"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

}

androidPublish {
    nameStyle = listOf(NameSegment.VersionName, NameSegment.Favor)
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
    implementation(libs.androidx.viewpager2)

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

    implementation(libs.bundles.themeUtil)

    implementation(libs.storageUtilities)
    implementation(libs.musicMetadataSource)
    implementation(libs.menuDsl)
    implementation(libs.seekArc)
    implementation(libs.slidingUpPanel)

    implementation(libs.bundles.materialDialogs)
    implementation(libs.composeMaterialDialogs)

    implementation(libs.okhttp3)
    implementation(libs.retrofit2)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.koin)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.licensesdialog)
    implementation(libs.jaudiotagger)
    implementation(libs.observablescrollview)
    implementation(libs.appIntro)
    implementation(libs.advrecyclerview)
    implementation(libs.recyclerviewFastscroll)
    implementation(libs.composeReorderable)
    implementation(libs.statusBarLyricsApi)
    implementation(libs.lyricsGetterAPi)

}
