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

tasks.create("cleanProject", type = Delete::class) {
    delete(rootProject.buildDir)
}
