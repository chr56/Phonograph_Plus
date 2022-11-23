import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationBaseFlavor
import version.management.CopyArtifactsTask
import version.management.Util.getGitHash
import version.management.Util.shiftFirstLetter
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("version.management")
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
        versionCode = 403
        versionName = "0.5-dev2"

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

            val cfg = CopyArtifactsTask.Config(
                variantName = variant.name,
                isRelease = variant.buildType == "release",
                appName = appName,
                versionName = (android.defaultConfig as ApplicationBaseFlavor).versionName ?: "N/A",
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

    composeOptions {
        kotlinCompilerExtensionVersion = deps.versions.composeCompiler.get()
    }

    kotlinOptions {
        jvmTarget = "1.8"
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

    implementation(deps.androidx.core)
    implementation(deps.androidx.appcompat)
    implementation(deps.androidx.activity)
    implementation(deps.androidx.fragment)
    implementation(deps.androidx.lifecycle.runtime)

    implementation(deps.androidx.annotation)
    implementation(deps.androidx.preference)

    implementation(deps.androidx.recyclerview)

    implementation(deps.androidx.constraintlayout)
    implementation(deps.androidx.percentlayout)
    implementation(deps.androidx.swiperefreshlayout)

    implementation(deps.androidx.media)
    implementation(deps.androidx.cardview)
    implementation(deps.androidx.palette)

    implementation(deps.google.material)

    implementation(deps.bundles.compose)
    debugImplementation(deps.compose.ui.tooling)

    implementation(deps.bundles.materialTools)

    implementation(deps.menuDsl)
    implementation(deps.seekArc)
    implementation(deps.slidingUpPanel)

    implementation(deps.bundles.materialDialogs)

    implementation(deps.okhttp3)
    implementation(deps.retrofit2)
    implementation(deps.coil)

    implementation(deps.kotlinx.serialization.json)

    implementation(deps.licensesdialog)
    implementation(deps.jaudiotagger)
    implementation(deps.observablescrollview)
    implementation(deps.materialIntro)
    implementation(deps.advrecyclerview)
    implementation(deps.recyclerviewFastscroll)
}
