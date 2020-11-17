package me.mattco.reevatools

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class ReevaToolsGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(ReevaToolsGradlePlugin::class.java)
    }

    override fun getCompilerPluginId(): String = "me.mattco.reeva-tools"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "me.mattco",
        artifactId = "reeva-tools",
        version = "0.1.0"
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val extension = project.extensions.findByType(ReevaToolsGradleExtension::class.java)
            ?: ReevaToolsGradleExtension()

        return project.provider { emptyList() }
    }

    override fun apply(target: Project): Unit = with(target) {
        extensions.create("reevaTools", ReevaToolsGradleExtension::class.java)
    }
}
