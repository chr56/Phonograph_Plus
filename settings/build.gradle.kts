import version.management.Deps

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("version.management") // Phonograph Plus's dependency management
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
        namespace = "lib.phonograph.setting"
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

        resValues = false
        aidl = false
        renderScript = false
        shaders = false
    }
}

dependencies {
    implementation(project(":common"))

    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.annotation)
    implementation(Deps.AndroidX.preference)
}
