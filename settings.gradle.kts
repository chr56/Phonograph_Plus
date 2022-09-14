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
includeBuild(file("version-management"))
include(":common",":coil")
