package me.mattco.reevatools

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ReevaToolsAnnotationTransformer(
    private val context: IrPluginContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    private val propertyKeySymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.PropertyKey"))!!
    private val jsValueSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.JSValue"))!!
    private val jsObjectSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.JSObject"))!!
    private val jsSymbolSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.primitives.JSSymbol"))!!
    private val realmCompanionSymbol = context.referenceClass(FqName("me.mattco.reeva.core.Realm"))!!.owner.companionObject() as IrClass
    private val listSymbol = context.referenceClass(FqName("kotlin.collections.List"))!!
    private val defineNativeFunctionSymbol = jsObjectSymbol.functions.first { it.owner.name == Name.identifier("defineNativeFunction") }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
        println(ir2stringWhole(irFile))
        println("\n\n===============")
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val funObject = buildFun {
            name = Name.identifier("annotationInit")
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
        }.also {
            declaration.declarations.removeIf { func -> func is IrSimpleFunction && func.name == it.name }
            declaration.declarations.add(it)
        }

        funObject.apply {
            parent = declaration
            addDispatchReceiver {
                name = declaration.name
                type = declaration.defaultType
            }

            body = DeclarationIrBuilder(context, currentScope!!.scope.scopeOwnerSymbol).run {
                at(declaration)

                irBlockBody {
                    declaration.functionsWithAnnoName("JSMethod").forEach { (function, ann) ->
                        +buildDefineNativeFunctionCall(function, ann, funObject.dispatchReceiverParameter!!)
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
        ann: IrConstructorCall,
        dispatchReceiverParameter: IrValueParameter
    ) = irCall(
        defineNativeFunctionSymbol,
        context.irBuiltIns.unitType
    ).apply {
        dispatchReceiver = irGet(dispatchReceiverParameter)

        putValueArgument(0, getPropertyKeyTarget(ann.getValueArgument(0)!!))
        putValueArgument(1, ann.getValueArgument(1))
        putValueArgument(2, ann.getValueArgument(2) ?: irInt(0b111111))

        putValueArgument(3, IrFunctionReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.kFunction(2).createType(false, listOf(
                makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT),
                makeTypeProjection(IrSimpleTypeBuilder().run {
                    classifier = listSymbol
                    kotlinType = KotlinTypeFactory.simpleNotNullType(
                        Annotations.EMPTY,
                        context.builtIns.list,
                        listOf(jsValueSymbol.defaultType.toKotlinType().asTypeProjection())
                    )
                    buildSimpleType()
                }, Variance.INVARIANT),
                makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT)
            )),
            targetFunction.symbol,
            typeArgumentsCount = 0,
            reflectionTarget = null,
            origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
        ).apply {
            this.dispatchReceiver = irGet(dispatchReceiverParameter)
        })
    }

    private fun IrBuilderWithScope.getPropertyKeyTarget(input: IrExpression): IrExpression {
        if (input !is IrConst<*> || input.kind != IrConstKind.String)
            TODO()

        val name = input.value as String
        var isSymbol = false
        val expr = if (name.startsWith("@@")) {
            val symbolProperty = realmCompanionSymbol.properties.firstOrNull {
                it.name.identifier == name
            } ?: TODO()
            isSymbol = true
            irGet(symbolProperty.getter!!.returnType, irGetObject(realmCompanionSymbol.symbol), symbolProperty.getter!!.symbol)
        } else input

        val ctor = propertyKeySymbol.constructors.first {
            it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().type.getClass()?.name?.identifier?.equals(
                if (isSymbol) "JSSymbol" else "String"
            ) == true
        }

        return irCallConstructor(ctor, listOf()).apply {
            putValueArgument(0, expr)
        }
    }
}
