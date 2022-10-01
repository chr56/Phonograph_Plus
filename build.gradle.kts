plugins {
    id("com.github.ben-manes.versions") version "0.42.0" apply false // enable if needed
    id("com.android.application") version "7.3.0" apply false
    // id("com.android.library") version "7.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.7.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10" apply false
}
repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}