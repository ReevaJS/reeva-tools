package me.mattco.reevatools

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ReevaToolsAnnotationTransformer(
    private val context: IrPluginContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {
//    private val defineNativeFunctionSymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeFunction"))
//        .first()
//    private val defineNativeAccessorSymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeAccessor"))
//        .first()
//    private val defineNativePropertySymbol = context.referenceFunctions(FqName("me.mattco.reeva.runtime.objects.JSObject.defineNativeProperty"))
//        .first()

    private val propertyKeySymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.PropertyKey"))!!
    private val jsValueSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.JSValue"))!!
    private val jsObjectSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.JSObject"))!!
    private val defineNativeFunctionSymbol = jsObjectSymbol.functions.first { it.owner.name == Name.identifier("defineNativeFunction") }

//    private val nativeFunctionType = createFunctionType(
//        context.builtIns,
//        Annotations.EMPTY,
//        null,
//        listOf(
//            KotlinTypeFactory.simpleNotNullType(
//                Annotations.EMPTY,
//                context.builtIns.list,
//                listOf(jsValueSymbol.owner.defaultType.toKotlinType().asTypeProjection())
//            )
//        ),
//        listOf(Name.identifier("thisValue"), Name.identifier("arguments")),
//        jsValueSymbol.owner.defaultType.toKotlinType()
//    )

    override fun lower(irFile: IrFile) {
        println("\n\n===============")
        println(ir2stringWhole(irFile))
        println("\n\n===============")
        irFile.transformChildrenVoid()
        println(ir2stringWhole(irFile))
        println("\n\n===============")
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val funObject = declaration.functions.find { it.name == Name.identifier("annotationInit") } ?: buildFun {
            name = Name.identifier("annotationInit")
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
        }.also { declaration.declarations.add(it) }

        funObject.apply {
            parent = declaration

            body = DeclarationIrBuilder(context, currentScope!!.scope.scopeOwnerSymbol).run {
                at(declaration)

                irBlockBody {
                    declaration.functionsWithAnnoName("JSMethod").forEach { (function, ann) ->
                        +buildDefineNativeFunctionCall(function, ann)
                    }
                }
            }
        }

        return super.visitClassNew(declaration)
    }

    private fun IrClass.functionsWithAnnoName(name: String): Sequence<Pair<IrSimpleFunction, IrConstructorCall>> =
        functions.mapNotNull {
            val ann = it.annotations.firstOrNull { ann -> ann.type.getClass()?.name?.equals(Name.identifier(name)) == true }
                ?: return@mapNotNull null
            it to ann
        }

    private fun IrBuilderWithScope.buildDefineNativeFunctionCall(
        targetFunction: IrSimpleFunction,
        ann: IrConstructorCall
    ) = irCall(
        defineNativeFunctionSymbol,
        context.irBuiltIns.unitType
    ).apply {
        putValueArgument(0, irCallConstructor(propertyKeySymbol.constructors.first {
            it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().type == context.irBuiltIns.stringType
        }, listOf(context.irBuiltIns.stringType)).apply {
            putValueArgument(0, ann.getValueArgument(0))
        })

        putValueArgument(1, ann.getValueArgument(1))

        putValueArgument(2, ann.getValueArgument(2) ?: irInt(0b111111))

        putValueArgument(3, IrFunctionReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            jsValueSymbol.defaultType,
            targetFunction.symbol,
            typeArgumentsCount = 0,
            reflectionTarget = null,
            origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
        ))
    }
}
