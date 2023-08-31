import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

repositories {
    google()
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.androidGradlePluginAPI)
}

gradlePlugin {
    plugins {
        create("release-tool") {
            id = "tools.release"
            implementationClass = "tools.release.Plugin"
        }
    }
}
