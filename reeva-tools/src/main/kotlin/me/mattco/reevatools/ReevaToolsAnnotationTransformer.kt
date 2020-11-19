package me.mattco.reevatools

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
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
    private val attributeCache = mutableMapOf<String, Int>()

    private val propertyKeySymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.PropertyKey"))!!
    private val jsValueSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.JSValue"))!!
    private val jsObjectSymbol = context.referenceClass(FqName("me.mattco.reeva.runtime.objects.JSObject"))!!
    private val realmCompanionSymbol = context.referenceClass(FqName("me.mattco.reeva.core.Realm"))!!.owner.companionObject() as IrClass
    private val listSymbol = context.referenceClass(FqName("kotlin.collections.List"))!!
    private val defineNativeFunctionSymbol = jsObjectSymbol.functions.first { it.owner.name == Name.identifier("defineNativeFunction") }
    private val defineNativeAccessorSymbol = jsObjectSymbol.functions.first { it.owner.name == Name.identifier("defineNativeAccessor") }
    private val defineNativePropertySymbol = jsObjectSymbol.functions.first { it.owner.name == Name.identifier("defineNativeProperty") }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.isSubclassOf(jsObjectSymbol.owner)) {
            val hasAnnotations = declaration.functions.any { func ->
                func.annotations.mapNotNull { it.type.getClass()?.name?.identifier }.any {
                    it == "JSMethod" || it == "JSNativePropertyGetter" || it == "JSNativePropertySetter" ||
                        it == "JSNativeAccessorGetter" || it == "JSNativeAccessorSetter"
                }
            }

            if (hasAnnotations) {
                throw ReevaCompilerPluginException("Reeva object annotations are only valid in subclasses of " +
                    "JSObject, but they are present in class ${declaration.name.identifier}")
            }
        }

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

                    val accessorFunctions = mutableMapOf<String, NativelyAnnotatedFunctions>()
                    declaration.functionsWithAnnoName("JSNativeAccessorGetter").forEach { (function, ann) ->
                        accessorFunctions[verifyConstKind(ann.getValueArgument(0)!!)] =
                            NativelyAnnotatedFunctions(AnnotatedFunction(function, ann), null)
                    }
                    declaration.functionsWithAnnoName("JSNativeAccessorSetter").forEach { (function, ann) ->
                        accessorFunctions.getOrPut(verifyConstKind(ann.getValueArgument(0)!!)) {
                            NativelyAnnotatedFunctions(null, null)
                        }.also {
                            it.setter = AnnotatedFunction(function, ann)
                        }
                    }

                    val propertyFunctions = mutableMapOf<String, NativelyAnnotatedFunctions>()
                    declaration.functionsWithAnnoName("JSNativePropertyGetter").forEach { (function, ann) ->
                        propertyFunctions[verifyConstKind(ann.getValueArgument(0)!!)] =
                            NativelyAnnotatedFunctions(AnnotatedFunction(function, ann), null)
                    }
                    declaration.functionsWithAnnoName("JSNativePropertySetter").forEach { (function, ann) ->
                        propertyFunctions.getOrPut(verifyConstKind(ann.getValueArgument(0)!!)) {
                            NativelyAnnotatedFunctions(null, null)
                        }.also {
                            it.setter = AnnotatedFunction(function, ann)
                        }
                    }

                    accessorFunctions.forEach { (_, functions) ->
                        +buildDefineNativeGetterSetterCall(functions, funObject.dispatchReceiverParameter!!, false)
                    }

                    propertyFunctions.forEach { (_, functions) ->
                        +buildDefineNativeGetterSetterCall(functions, funObject.dispatchReceiverParameter!!, true)
                    }
                }
            }
        }

        return super.visitClassNew(declaration)
    }

    private fun IrClass.functionsWithAnnoName(name: String): Sequence<Pair<IrSimpleFunction, IrConstructorCall>> =
        functions.filterNot { it.isFakeOverride }.mapNotNull {
            val ann = it.annotations.firstOrNull { ann -> ann.type.getClass()?.name?.equals(Name.identifier(name)) == true }
                ?: return@mapNotNull null
            it to ann
        }

    /**
     * Asserts that an IrExpression is an instance of IrConst<T>,
     * and returns the value of that constant
     */
    private inline fun <reified T> verifyConstKind(expr: IrExpression): T {
        if (expr !is IrConst<*>)
            throw ReevaCompilerPluginException("Expected IrConst<${T::class}>, got ${expr.dump(normalizeNames = true)}")
        val value = expr.value
        if (value !is T)
            throw ReevaCompilerPluginException("Expected IrConst<${T::class}>, got ${expr.dump(normalizeNames = true)}")
        return value
    }

    /**
     * Given a block of code that looks like the following:
     *
     *     @JSMethod("methodName", 2, Descriptor.CONFIGURABLE)
     *     fun methodName(thisValue: JSValue, arguments: List<JSValue>): JSValue {
     *         // ...
     *     }
     *
     * ...this function adds the following code to the object's annotationInit method:
     *
     *     defineNativeMethod(PropertyKey("methodName"), 2, Descriptor.CONFIGURABLE, ::methodName)
     *
     * If the method name argument of the @JSMethod annotation begins with "@@", this function
     * will instead generate the following code:
     *
     *     defineNativeMethod(PropertyKey(Realm.`@@methodName`), 2, Descriptor.CONFIGURABLE, ::methodName)
     */
    private fun IrBuilderWithScope.buildDefineNativeFunctionCall(
        targetFunction: IrSimpleFunction,
        ann: IrConstructorCall,
        dispatchReceiverParameter: IrValueParameter
    ) = irCall(
        defineNativeFunctionSymbol,
        context.irBuiltIns.unitType,
    ).apply {
        dispatchReceiver = irGet(dispatchReceiverParameter)

        putValueArgument(0, getPropertyKeyTarget(ann.getValueArgument(0)!!))
        putValueArgument(1, ann.getValueArgument(1))
        val attrs = ann.getValueArgument(2)?.let {
            makeAttributes(verifyConstKind(it))
        } ?: irInt(0b111101)
        putValueArgument(2, attrs)
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

    /**
     * Given a block of code that looks like the following:
     *
     *     @JSNativeAccessorGetter("propertyName", Descriptor.CONFIGURABLE)
     *     fun getPropertyName(thisValue: JSValue): JSValue {
     *         // ...
     *     }
     *
     *     @JSNativeAccessorSetter("propertyName", Descriptor.CONFIGURABLE)
     *     fun setPropertyName(thisValue: JSValue, newValue: JSValue) {
     *         // ...
     *     }
     *
     *
     * ...this function adds the following code to the object's annotationInit method:
     *
     *     defineNativeAccessor(PropertyKey("propertyName"), Descriptor.CONFIGURABLE, ::getPropertyName, ::setPropertyName)
     *
     * If the method name argument of the @JSMethod annotation begins with "@@", this function
     * will instead generate the following code:
     *
     *     defineNativeAccessor(PropertyKey(Realm.`@@propertyName`), Descriptor.CONFIGURABLE, ::getPropertyName, ::setPropertyName)
     *
     * If there is only a getter or setter, that function will be passed to the defineNativeAccessor
     * method along with "null" for the setter or getter, respectively.
     *
     * This method also generates code for the @JSNativeProperty{Getter,Setter} annotations,
     * which follow the same rules as the above explanation, but with the function that is
     * called being "defineNativeProperty" instead of "defineNativeAccessor"
     */
    private fun IrBuilderWithScope.buildDefineNativeGetterSetterCall(
        functions: NativelyAnnotatedFunctions,
        dispatchReceiverParameter: IrValueParameter,
        isProperty: Boolean,
    ) = irCall(
        if (isProperty) defineNativePropertySymbol else defineNativeAccessorSymbol,
        context.irBuiltIns.unitType,
    ).apply {
        dispatchReceiver = irGet(dispatchReceiverParameter)

        val (getterPair, setterPair) = functions

        putValueArgument(0, getPropertyKeyTarget((getterPair ?: setterPair)!!.anno.getValueArgument(0)!!))
        // TODO: Verify attributes are the same?
        val attrs = (getterPair ?: setterPair)!!.anno.getValueArgument(1)?.let {
            makeAttributes(verifyConstKind(it))
        } ?: irInt(0b111101)
        putValueArgument(1, attrs)

        putValueArgument(2, getterPair?.function?.let {
            IrFunctionReferenceImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.irBuiltIns.kFunction(1).createType(false, listOf(
                    makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT),
                    makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT)
                )),
                it.symbol,
                typeArgumentsCount = 0,
                reflectionTarget = null,
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            ).apply {
                this.dispatchReceiver = irGet(dispatchReceiverParameter)
            }
        } ?: irNull())
        putValueArgument(3, setterPair?.function?.let {
            IrFunctionReferenceImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.irBuiltIns.kFunction(2).createType(false, listOf(
                    makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT),
                    makeTypeProjection(jsValueSymbol.defaultType, Variance.INVARIANT),
                    makeTypeProjection(context.irBuiltIns.unitType, Variance.INVARIANT),
                )),
                it.symbol,
                typeArgumentsCount = 0,
                reflectionTarget = null,
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            ).apply {
                this.dispatchReceiver = irGet(dispatchReceiverParameter)
            }
        } ?: irNull())
    }

    /**
     * Maps a IrExpression which is a IrConst<String> into either
     * itself (if it represents a normal string property name), or
     * a reference to a Realm property (if it starts with "@@")
     *
     * Examples, using Kotlin syntax instead of IrExpression objects:
     *     getPropertyKeyTarget("myProperty") => "myProperty"
     *     getPropertyKeyTarget("@@myProperty") => Realm.`@@myProperty`
     */
    private fun IrBuilderWithScope.getPropertyKeyTarget(input: IrExpression): IrExpression {
        val name = verifyConstKind<String>(input)
        var isSymbol = false
        val expr = if (name.startsWith("@@")) {
            val symbolProperty = realmCompanionSymbol.properties.firstOrNull {
                it.name.identifier == name
            } ?: throw ReevaCompilerPluginException("Unable to find a static symbol property on Realm with name $name")
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

    private fun IrBuilderWithScope.makeAttributes(attrs: String): IrExpression {
        return attributeCache.getOrPut(attrs) {
            validateAttributes(attrs)

            // TODO: Don't hardcode constants?
            var attr = 0

            attrs.forEach {
                attr = attr or (when (it) {
                    'C' -> (1 shl 0) or (1 shl 3)
                    'E' -> (1 shl 1) or (1 shl 4)
                    'W' -> (1 shl 2) or (1 shl 5)
                    'c' -> 1 shl 3
                    'e' -> 1 shl 4
                    'w' -> 1 shl 5
                    else -> throw Error()
                })
            }

            attr
        }.let(::irInt)
    }

    private fun validateAttributes(attrs: String) {
        if (attrs.isEmpty() || attrs.length > 3)
            throw ReevaCompilerPluginException("Annotation attributes must be a string of length 1..3")
        if ('c' in attrs && 'C' in attrs)
            throw ReevaCompilerPluginException("Annotation attributes cannot specify 'c' and 'C'")
        if ('e' in attrs && 'E' in attrs)
            throw ReevaCompilerPluginException("Annotation attributes cannot specify 'e' and 'E'")
        if ('w' in attrs && 'W' in attrs)
            throw ReevaCompilerPluginException("Annotation attributes cannot specify 'w' and 'W'")
        if (attrs.any { it !in listOf('c', 'C', 'e', 'E', 'w', 'W') })
            throw ReevaCompilerPluginException("Annotation attributes cannot contain characters that are not 'c', 'C', 'e', 'E', 'w', or 'W'")
    }

    data class AnnotatedFunction(
        val function: IrSimpleFunction,
        val anno: IrConstructorCall
    )

    data class NativelyAnnotatedFunctions(
        var getter: AnnotatedFunction?,
        var setter: AnnotatedFunction?,
    )
}
