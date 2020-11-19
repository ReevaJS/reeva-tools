plugins {
    kotlin("jvm") version "1.4.10" apply false
    id("org.jetbrains.dokka") version "0.10.0" apply false
    id("com.gradle.plugin-publish") version "0.11.0" apply false
}

allprojects {
    group = "me.mattco"
    version = "0.2.0"
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
    }
}
