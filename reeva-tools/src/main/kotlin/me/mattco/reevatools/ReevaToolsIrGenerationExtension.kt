package me.mattco.reevatools

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class ReevaToolsIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            ReevaToolsAnnotationTransformer(pluginContext).lower(file)
        }
    }
}
