import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

repositories {
    mavenCentral()
    google()
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

val originalReleaseNotePath = "ReleaseNote.md"

val outputGitHubReleaseNotePath = "GitHubReleaseNote.md"
val outputEncodedUrlPath = "GitHubReleaseNote.url.txt"

val changelogsPath = "app/src/main/assets"

tasks.register("GenerateGithubReleaseNote", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        outputGitHubReleaseNotePath
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateGithubReleaseNoteKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("GenerateEncodedUrl", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        outputEncodedUrlPath
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainEncodeUrlKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("GenerateHTML", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateHtmlKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("RefreshChangelogs", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        changelogsPath
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainRefreshChangelogsKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("GenerateVersionJson", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        "version_catalog.json"
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateVersionJsonKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("GenerateFdroidMetadata", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateFdroidMetadataKt")

    dependsOn(tasks.findByPath("build"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType(KotlinCompile::class.java) {
    (kotlinOptions as KotlinJvmOptions).jvmTarget = "17"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}