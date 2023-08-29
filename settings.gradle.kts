dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://jitpack.io")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.buildFileName = "build.gradle.kts"

include(":app")
include(":tools:changelog-generator")
includeBuild(file("tools/release-tool"))
