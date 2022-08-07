/*
 * Copyright (c) 2022 chr_56
 */

repositories {
    mavenCentral()
    google()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("java-gradle-plugin")
}

java {
    sourceSets["main"].java.srcDirs("src/java")
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("version-management") {
            id = "version.management"
            implementationClass = "version.management.Plugin"
        }
    }
}
