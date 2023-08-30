dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://jitpack.io")
    }

    versionCatalogs {
        create("plugins") {
            from(files("./gradle/plugins.versions.toml"))
        }
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
