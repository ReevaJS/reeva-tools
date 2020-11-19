import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")

    id("maven-publish")
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create("reevaTools") {
            id = "me.mattco.reeva-tools"
            displayName = "Reeva Tools Plugin"
            description = "Kotlin Compiler Plugin for use in the Reeva JS Engine"
            implementationClass = "me.mattco.reevatools.ReevaToolsGradlePlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "nexus"
            url = URI("https://repo.sk1er.club/repository/maven-releases/")

            credentials {
                username = project.findProperty("nexus_user") as String
                password = project.findProperty("nexus_password") as String
            }
        }
    }
}
