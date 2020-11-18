package me.mattco.reeva.runtime.objects

import me.mattco.reeva.runtime.JSValue

open class JSObject : JSValue() {
    fun defineNativeFunction(key: PropertyKey, arity: Int, descriptor: Int, function: (JSValue, List<JSValue>) -> JSValue) {
        println("Successfully defined function ${key.value} (arity: $arity, descriptor: $descriptor) to point to $function")
    }
    fun defineNativeAccessor(key: PropertyKey, descriptor: Int, getter: ((JSValue) -> JSValue)?, setter: ((JSValue, JSValue) -> JSValue)?) {
        println("Successfully defined accessor ${key.value} (descriptor: $descriptor) to point to $getter and $setter")
    }
    fun defineNativeProperty(key: PropertyKey, descriptor: Int, getter: ((JSValue) -> JSValue)?, setter: ((JSValue, JSValue) -> JSValue)?) {
        println("Successfully defined accessor ${key.value} (descriptor: $descriptor) to point to $getter and $setter")
    }

    open fun annotationInit() {
        println("Base ann init :(")
    }

    companion object {
        const val ATTRIBUTE = 5
    }
}
