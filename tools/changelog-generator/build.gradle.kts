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

tasks.register("GenerateVersionJson", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        originalReleaseNotePath,
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateVersionJsonKt")

    dependsOn(tasks.findByPath("build"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies{
    implementation(depsLibs.kotlinx.serialization.json)
}