import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    val agpVersion = "7.4.2"
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:$agpVersion")
}

gradlePlugin {
    plugins {
        create("release-tool") {
            id = "tools.release"
            implementationClass = "tools.release.Plugin"
        }
    }
}
