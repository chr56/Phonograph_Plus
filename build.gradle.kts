buildscript {
    val kotlinVersion by extra { "1.6.20" }
    @Suppress("JcenterRepositoryObsolete")
    repositories {
        mavenCentral()
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.0")
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

tasks.create("cleanProject", type = Delete::class) {
    delete(rootProject.buildDir)
}
