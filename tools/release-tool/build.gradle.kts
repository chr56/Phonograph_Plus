repositories {
    google()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.8.10"
    id("java-gradle-plugin")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    val agp = "7.4.1"
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:$agp")
}

gradlePlugin {
    plugins {
        create("release-tool") {
            id = "tools.release"
            implementationClass = "tools.release.Plugin"
        }
    }
}
