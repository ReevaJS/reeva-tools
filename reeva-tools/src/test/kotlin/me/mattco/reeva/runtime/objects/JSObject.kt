package me.mattco.reeva.runtime.objects

import me.mattco.reeva.runtime.JSValue

open class JSObject : JSValue() {
    fun defineNativeFunction(key: PropertyKey, arity: Int, descriptor: Int, function: (JSValue, List<JSValue>) -> JSValue) {
        println("Successfully defined ${key.value} (arity: $arity, descriptor: $descriptor) to point to $function")
    }

    open fun annotationInit() {
        println("Base ann init :(")
    }
}
