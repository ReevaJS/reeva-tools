package me.mattco.reevatools

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.expressions.builder.buildLambdaArgumentExpression
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ReevaToolsAnnotationTransformer(
    private val context: IrPluginContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {
//    private val defineNativeFunctionSymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeFunction")).first()
//    private val defineNativeAccessorSymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeAccessor")).first()
//    private val defineNativePropertySymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeProperty")).first()
//
//    private val propertyKeySymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.PropertyKey"))
//    private val t = context.referenceClass(FqName())

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        println(ir2string(declaration))
        return super.visitFunctionNew(declaration)
    }

    override fun visitClassNew(clazz: IrClass): IrStatement {
        buildFun {
            name = Name.identifier("annotationInit")
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
        }.apply {
            parent = clazz

            body = DeclarationIrBuilder(context, currentScope!!.scope.scopeOwnerSymbol).run {
                at(clazz)

                irBlockBody {
                    clazz.functionsWithAnnoName("JSMethod").forEach { (function, ann) ->
                        buildDefineNativeFunctionCall(function, ann)
                    }
                }
            }
        }

        build

        return super.visitClassNew(clazz)
    }

    private fun IrClass.functionsWithAnnoName(name: String): Sequence<Pair<IrSimpleFunction, IrConstructorCall>> = functions.mapNotNull {
        val ann = it.annotations.firstOrNull { ann -> ann.type.getClass()?.name?.equals(name) == true } ?: return@mapNotNull null
        it to ann
    }

    private fun IrSimpleFunction.firstNamedAnno(name: IrConstructorCall) = annotations.firstOrNull {
        it.type.getClass()?.name?.equals(name) == true
    }

    private fun IrBuilderWithScope.buildDefineNativeFunctionCall(targetFunction: IrSimpleFunction, ann: IrConstructorCall) {
//        buildStatement(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
//            ir2st
//        }
    }

    companion object {
        val TARGET_SUPER_CLASS_NAME = FqName("club.sk1er.elementa.UIComponent")
    }
}
