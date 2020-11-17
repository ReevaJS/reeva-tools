import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")

    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

pluginBundle {
    website = "https://github.com/mattco98/ReevaTools"
    vcsUrl = "https://github.com/mattco98/ReevaTools.git"
    tags = listOf("kotlin", "elementa")
}

gradlePlugin {
    plugins {
        create("reevaTools") {
            id = "me.mattco.reeva-tools"
            displayName = "Reeva Tools Plugin"
            description = "Kotlin Compiler Plugin for use in the Reeva JS Engine"
            implementationClass = "me.mattco.reevatools.ElementaToolsGradlePlugin"
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register("publish") {
    dependsOn("publishPlugins")
}
