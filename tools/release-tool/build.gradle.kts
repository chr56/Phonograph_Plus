repositories {
    google()
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("java-gradle-plugin")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    val agp = "7.3.0"
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
