
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

fun getGitHash(type: Int): String {
    val stdout = ByteArrayOutputStream()
    if (type == 1) exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    } else exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

val isSigningFileExist: Boolean = rootProject.file("signing.properties").exists()
var signingProperties = Properties()
if (isSigningFileExist) {
    signingProperties.load(FileInputStream(rootProject.file("signing.properties")))
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    defaultConfig {
        minSdk = (24)
        targetSdk = (31)

        renderscriptTargetApi = 29
        vectorDrawables.useSupportLibrary = true

        applicationId = "player.phonograph"
        versionCode = 200
        versionName = "0.2.0"

        buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitHash(1)}\"")
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
//    afterEvaluate {
//        applicationVariants.forEach { variant ->
//            if (variant.name == "release") {
//                variant.outputs.all {
//                    this.outputFileName = "PhonographPlus_${variant.versionName}.apk"
//                }
//            } else {
//                variant.outputs.all {
//                    this.outputFileName = "${variant.name}_${variant.versionName}_${Calendar.getInstance().time}.apk" // todo
//                }
//            }
//        }
//    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles.apply {
                add(File("proguard-rules-base.pro"))
                add(File("proguard-rules-app.pro"))
            }
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".plus"
            resValue("string", "app_name", "Phonograph Plus")
        }
        debug {
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            applicationIdSuffix = ".plus.debug"
            resValue("string", "app_name", "Phonograph Plus Debug")
        }
        create("preview") {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles.apply {
                add(File("proguard-rules-base.pro"))
                add(File("proguard-rules-app.pro"))
            }

            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            resValue("string", "app_name", "Phonograph Plus Preview")
            applicationIdSuffix = ".plus.preview"
            matchingFallbacks.add("release")
        }
        create("proguard") {
            isMinifyEnabled = true
            proguardFiles.apply {
                add(File("proguard-rules-base.pro"))
                add(File("proguard-rules-app.pro"))
            }
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            resValue("string", "app_name", "Phonograph Plus Proguard")
            applicationIdSuffix = ".plus.proguard"
            matchingFallbacks.add("release")
            isDebuggable = true
        }
        create("checkout") {
            // for checkout
            if (isSigningFileExist) signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            proguardFiles.apply {
                add(File("proguard-rules-base.pro"))
                add(File("proguard-rules-app.pro"))
            }
            resValue("string", "app_name", "Phonograph Plus Checkout")
            applicationIdSuffix = ".plus.checkout"
            matchingFallbacks.add("release")
            isDebuggable = true
        }
        // for ci
        create("ci") {
            applicationIdSuffix = ".plus.ci"
            resValue("string", "app_name", "Phonograph Plus CI Build")
            matchingFallbacks.add("release")
            isDebuggable = true
        }
    }
    packagingOptions {
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
    }
    lint {
        abortOnError = false
        disable.add("MissingTranslation")
        disable.add("InvalidPackage")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    /*
    afterEvaluate {
        tasks.withType(JavaCompile::class) {
            options.compilerArgs.add(" -Xlint:deprecation")
            options.compilerArgs.add(" -Xlint:unchecked")
        }
    }
    */
}

@Suppress("SpellCheckingInspection")
dependencies {

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")

    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.media:media:1.5.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("com.google.android.material:material:1.4.0")

    implementation("com.github.chr56:mdUtil:0.0.1")
    implementation("com.github.chr56:mdColor:0.0.1")

    implementation("com.github.kabouzeid:RecyclerView-FastScroll:1.0.16-kmod")
    implementation("com.github.chr56:SeekArc:c5ae37866e")
    implementation("com.github.kabouzeid:AndroidSlidingUpPanel:6")
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad.material-dialogs:input:3.3.0")
    implementation("com.afollestad.material-dialogs:color:3.3.0")
    implementation("com.afollestad.material-dialogs:files:3.3.0")
    implementation("com.afollestad:material-cab:2.0.1")

    implementation("com.github.ksoichiro:android-observablescrollview:1.6.0")
    implementation("com.heinrichreimersoftware:material-intro:2.0.0")
    implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.5.0")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation("de.psdev.licensesdialog:licensesdialog:2.1.0")

    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.12.0")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")

    implementation("com.github.AdrienPoupa:jaudiotagger:2.2.3")
}
