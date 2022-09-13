import com.android.build.api.artifact.SingleArtifact
import java.util.*
import version.management.CopyArtifactsTask
import version.management.CopyArtifactsTask.Config as CopyConfig
import version.management.Deps
import version.management.Util.getGitHash
import version.management.Util.shiftFirstLetter

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("version.management") // Phonograph Plus's dependency management
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
        viewBinding = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 33

        renderscriptTargetApi = 29
        vectorDrawables.useSupportLibrary = true

        applicationId = "player.phonograph"
        versionCode = 310
        versionName = "0.3.2"

        buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitHash(false)}\"")
        setProperty("archivesBaseName", "PhonographPlus_$versionName")
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
            proguardFiles(File("proguard-rules-base.pro"), File("proguard-rules-app.pro"))
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
        create("common") {
            dimension = "purpose"

            applicationIdSuffix = ".plus"
            resValue("string", "app_name", appName)
        }
        create("preview") {
            dimension = "purpose"
            matchingFallbacks.add("common")

            resValue("string", "app_name", "$appName Preview")
            applicationIdSuffix = ".plus.preview"
        }
        // test Proguard
        create("proguardTest") {
            dimension = "purpose"
            matchingFallbacks.add("common")

            resValue("string", "app_name", "$appName ProguardTest")
            applicationIdSuffix = ".plus.proguard"
        }
        // for checkout to locate a bug etc.
        create("checkout") {
            dimension = "purpose"
            matchingFallbacks.add("common")

            resValue("string", "app_name", "$appName Checkout")
            applicationIdSuffix = ".plus.checkout"
        }
        // for ci
        create("ci") {
            dimension = "purpose"
            matchingFallbacks.add("common")

            applicationIdSuffix = ".plus.ci"
            resValue("string", "app_name", "$appName CI Build")
        }
    }
    androidComponents {
        beforeVariants(selector().withBuildType("release")) { variantBuilder ->
            val favors = variantBuilder.productFlavors
            // no "release" type
            if (favors.contains("purpose" to "checkout") || favors.contains("purpose" to "ci")) {
                variantBuilder.enable = false
            }
        }
        beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
            val favors = variantBuilder.productFlavors
            // no "debug" type
            if (favors.contains("purpose" to "proguardTest")) {
                variantBuilder.enable = false
            }
        }

        onVariants(selector().withBuildType("release")) { variant ->
            val canonicalName = variant.name.shiftFirstLetter()

            val loader = variant.artifacts.getBuiltArtifactsLoader()

            val apkOutputDirectory = variant.artifacts.get(SingleArtifact.APK)
            val mappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)

            val fileListToCopy = ArrayList<File>()

            afterEvaluate {
                loader.load(apkOutputDirectory.get())?.also { builtArtifacts ->
                    fileListToCopy.addAll(
                        builtArtifacts.elements.map { File(it.outputFile) }
                    )
                }
                mappingFile.orNull?.asFile?.let {
                    fileListToCopy.add(it)
                }
            }

            val cfg = CopyConfig(
                variantName = variant.name,
                isRelease = variant.buildType == "release",
                appName = appName,
                versionName = (android.defaultConfig as com.android.build.api.dsl.ApplicationBaseFlavor).versionName ?: "N/A",
                gitHash = getGitHash(true),
                artifactsFiles = fileListToCopy
            )

            tasks.register("copyArtifactsFor$canonicalName", CopyArtifactsTask::class.java, cfg)

            tasks.register("Publish$canonicalName", CopyArtifactsTask::class.java, cfg).configure {
                dependsOn("assemble$canonicalName")
            }
        }
    }

    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("InvalidPackage")

        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

}

/**
 * see composing-build-module: [version.management.Deps]
 */
dependencies {

    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.appcompat)
    implementation(Deps.AndroidX.activity)
    implementation(Deps.AndroidX.fragment)
    implementation(Deps.AndroidX.lifecycle_runtime)

    implementation(Deps.AndroidX.annotation)
    implementation(Deps.AndroidX.preference)

    implementation(Deps.AndroidX.recyclerview)

    implementation(Deps.AndroidX.constraintlayout)
    implementation(Deps.AndroidX.percentlayout)
    implementation(Deps.AndroidX.swiperefreshlayout)

    implementation(Deps.AndroidX.media)
    implementation(Deps.AndroidX.cardview)
    implementation(Deps.AndroidX.palette)

    implementation(Deps.google_material)

    implementation(Deps.mdColorRes)
    implementation(Deps.mdUtil)
    implementation(Deps.mdPref)
    implementation(Deps.mdTint)

    implementation(Deps.android_menu_dsl)
    implementation(Deps.SeekArc)
    implementation(Deps.AndroidSlidingUpPanel_kabouzeid)

    implementation(Deps.recyclerview_fastscroll)

    implementation(Deps.material_dialogs_core)
    implementation(Deps.material_dialogs_input)
    implementation(Deps.material_dialogs_color)

    implementation(Deps.okhttp3)
    implementation(Deps.retrofit2)

    implementation(Deps.kotlinx_serialization_json)

    implementation(Deps.coil)

    implementation(Deps.licensesdialog)
    implementation(Deps.jaudiotagger)
    implementation(Deps.observablescrollview)
    implementation(Deps.material_intro)
    implementation(Deps.advrecyclerview)
}
