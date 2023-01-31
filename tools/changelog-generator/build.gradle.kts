repositories {
    mavenCentral()
    google()
}

plugins {
    kotlin("jvm")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

val releaseNotePath = "/tools/changelog-generator/FormatExample.md"

tasks.register("GenerateGithubReleaseNote", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
        releaseNotePath,
        "products/GitHubReleaseMarkDown.md"
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateGithubReleaseNoteKt")

    dependsOn(tasks.findByPath("build"))
}

tasks.register("GenerateHTML", JavaExec::class.java) {
    args = listOf(
        rootProject.projectDir.absolutePath,
       releaseNotePath,
    )

    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("util.phonograph.MainGenerateHtmlKt")

    dependsOn(tasks.findByPath("build"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}