package me.mattco.reevatools

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class ReevaToolsCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "me.mattco.reeva-tools"

    override val pluginOptions: Collection<CliOption> = listOf()
}
