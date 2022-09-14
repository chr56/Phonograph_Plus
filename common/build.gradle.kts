import version.management.Deps

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("version.management") // Phonograph Plus's dependency management
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
//        namespace = "lib.phonograph.common" // todo: rename namespace
        namespace = "player.phonograph"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {

    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.appcompat)
    implementation(Deps.AndroidX.activity)
    implementation(Deps.AndroidX.fragment)
    implementation(Deps.AndroidX.lifecycle_runtime)
    implementation(Deps.AndroidX.annotation)

    implementation(Deps.AndroidX.recyclerview)
    implementation(Deps.AndroidX.constraintlayout)

    implementation(Deps.google_material)

    implementation(Deps.okhttp3)
}
