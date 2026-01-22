import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

repositories {
    mavenCentral()
    google()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

val originalReleaseNotePath = "ReleaseNote.yaml"

val outputGitHubReleaseNotePath = "GitHubReleaseNote.md"
val outputEscapedReleaseNotePath = "EscapedReleaseNote.md"

val changelogsPath = "app/src/main/assets"

fun prepareTask(task: JavaExec) {
    with(task) {
        classpath = sourceSets.named("main").get().runtimeClasspath
        mainClass.set("util.phonograph.MainKt")

        dependsOn(tasks.findByPath("build"))
    }
}

tasks.register("GenerateGithubReleaseNote", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateGithubReleaseNote",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        outputGitHubReleaseNotePath
    )
}

tasks.register("GenerateEscapedMarkdownReleaseNote", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateEscapedMarkdownReleaseNote",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        outputEscapedReleaseNotePath
    )
}

tasks.register("GenerateHTML", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateHTML",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
    )
}

tasks.register("RefreshChangelogs", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "RefreshChangelogs",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        changelogsPath
    )
}

tasks.register("GenerateVersionJson", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateVersionJson",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        "version_catalog.json"
    )
}

tasks.register("GenerateFdroidMetadata", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateFdroidMetadata",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath
    )
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.kaml)
}