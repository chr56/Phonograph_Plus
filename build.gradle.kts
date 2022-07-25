buildscript {
    val kotlinVersion by extra { "1.6.21" }
    @Suppress("JcenterRepositoryObsolete")
    repositories {
        mavenCentral()
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    @Suppress("JcenterRepositoryObsolete")
    repositories {
        mavenCentral()
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks.create("encodeReleaseNoteToUrl") {

    val inputFile = File("ReleaseNote.md")
    val outputFile = File("ReleaseNote.url.txt")

    if (inputFile.exists()) {
        java.io.FileInputStream(inputFile).use { input ->
            val content = String(input.readAllBytes())
            val result = java.net.URLEncoder.encode(content, "UTF-8")
            java.io.FileOutputStream(outputFile).use { output ->
                output.write(result.toByteArray())
                output.flush()
            }
        }
    }
}
