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
        namespace = "lib.phonograph"
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":common"))

    implementation(Deps.AndroidX.core)
    implementation(Deps.AndroidX.annotation)
    implementation(Deps.AndroidX.palette)

    implementation(Deps.coil)
    implementation(Deps.okhttp3)

    implementation(Deps.jaudiotagger)
}
