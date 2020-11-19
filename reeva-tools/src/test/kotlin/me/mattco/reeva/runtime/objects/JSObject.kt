package me.mattco.reeva.runtime.objects

import me.mattco.reeva.core.Realm
import me.mattco.reeva.runtime.JSValue
import me.mattco.reeva.runtime.primitives.JSSymbol
import me.mattco.reeva.utils.key

open class JSObject() : JSValue() {
    fun defineNativeFunction(key: PropertyKey, arity: Int, descriptor: Int, function: (JSValue, List<JSValue>) -> JSValue) {
        println("Successfully defined function ${key.value} (arity: $arity, descriptor: $descriptor) to point to $function")
    }
    fun defineNativeAccessor(key: PropertyKey, descriptor: Int, getter: ((JSValue) -> JSValue)?, setter: ((JSValue, JSValue) -> JSValue)?) {
        println("Successfully defined accessor ${key.value} (descriptor: $descriptor) to point to $getter and $setter")
    }
    fun defineNativeProperty(key: PropertyKey, descriptor: Int, getter: ((JSValue) -> JSValue)?, setter: ((JSValue, JSValue) -> JSValue)?) {
        println("Successfully defined accessor ${key.value} (descriptor: $descriptor) to point to $getter and $setter")
    }

    open fun init() { }

    @JvmOverloads fun defineOwnProperty(property: String, value: JSValue, attributes: Int = Descriptor.defaultAttributes) = defineOwnProperty(property.key(), Descriptor(value, attributes))
    @JvmOverloads fun defineOwnProperty(property: JSSymbol, value: JSValue, attributes: Int = Descriptor.defaultAttributes) = defineOwnProperty(property.key(), Descriptor(value, attributes))
    @JvmOverloads fun defineOwnProperty(property: Long, value: JSValue, attributes: Int = Descriptor.defaultAttributes) = defineOwnProperty(property.toString().key(), Descriptor(value, attributes))

    open fun defineOwnProperty(property: PropertyKey, descriptor: Descriptor): Boolean {
        return false
    }

    open fun annotationInit() {
        println("Base ann init :(")
    }

    companion object {
        const val ATTRIBUTE = 5
    }
}
