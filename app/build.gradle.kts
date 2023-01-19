import com.android.build.api.dsl.ApplicationBaseFlavor
import tools.release.git.getGitHash
import tools.release.registerPublishTask
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
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
        viewBinding = true
        compose = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 33

        renderscriptTargetApi = 29
        vectorDrawables.useSupportLibrary = true

        applicationId = "player.phonograph.plus"
        versionCode = 453
        versionName = "0.5.3"

        buildConfigField("String",
                         "GIT_COMMIT_HASH",
                         """
                             "${getGitHash(false)}"
                         """.trimIndent())
        setProperty("archivesBaseName", "PhonographPlus_$versionName")

        proguardFiles(File("proguard-rules-base.pro"), File("proguard-rules-app.pro"))
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

        onVariants(selector().withBuildType("release")) { variant ->
            val version = (android.defaultConfig as ApplicationBaseFlavor).versionName ?: "N/A"
            tasks.registerPublishTask(appName, version, variant)
        }
    }

    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("InvalidPackage")

        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    composeOptions {
        kotlinCompilerExtensionVersion = depsLibs.versions.composeCompiler.get()
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
        )
    }

}

/**
 * Now, this project is using [VersionCatalog] (declaring in `setting.gradle.kts`)
 */
dependencies {

    implementation(depsLibs.androidx.core)
    implementation(depsLibs.androidx.appcompat)
    implementation(depsLibs.androidx.activity)
    implementation(depsLibs.androidx.fragment)
    implementation(depsLibs.androidx.lifecycle.runtime)

    implementation(depsLibs.androidx.annotation)
    implementation(depsLibs.androidx.preference)

    implementation(depsLibs.androidx.recyclerview)

    implementation(depsLibs.androidx.constraintlayout)
    implementation(depsLibs.androidx.percentlayout)
    implementation(depsLibs.androidx.swiperefreshlayout)

    implementation(depsLibs.androidx.media)
    implementation(depsLibs.androidx.cardview)
    implementation(depsLibs.androidx.palette)

    implementation(depsLibs.google.material)

    implementation(depsLibs.bundles.compose)
    debugImplementation(depsLibs.compose.ui.tooling)

    implementation(depsLibs.bundles.materialTools)

    implementation(depsLibs.menuDsl)
    implementation(depsLibs.seekArc)
    implementation(depsLibs.slidingUpPanel)

    implementation(depsLibs.bundles.materialDialogs)
    implementation(depsLibs.composeMaterialDialogs)

    implementation(depsLibs.okhttp3)
    implementation(depsLibs.retrofit2)
    implementation(depsLibs.coil)

    implementation(depsLibs.kotlinx.serialization.json)

    implementation(depsLibs.licensesdialog)
    implementation(depsLibs.jaudiotagger)
    implementation(depsLibs.observablescrollview)
    implementation(depsLibs.materialIntro)
    implementation(depsLibs.advrecyclerview)
    implementation(depsLibs.recyclerviewFastscroll)
    implementation(depsLibs.composeReorderable)
    implementation(depsLibs.statusBarLyricsApi)
}
