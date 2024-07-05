import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

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
val outputEncodedUrlPath = "GitHubReleaseNote.url.txt"
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

tasks.register("GenerateEncodedUrl", JavaExec::class.java) {
    prepareTask(this)
    args = listOf(
        "GenerateEncodedUrl",
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
        outputEncodedUrlPath
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType(KotlinCompile::class.java) {
    (kotlinOptions as KotlinJvmOptions).jvmTarget = "17"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.kaml)
}