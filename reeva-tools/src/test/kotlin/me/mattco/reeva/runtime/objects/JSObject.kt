package me.mattco.reeva.runtime.objects

import me.mattco.reeva.runtime.JSValue

open class JSObject : JSValue() {
    fun defineNativeFunction(key: PropertyKey, arity: Int, descriptor: Int, function: (List<JSObject>) -> JSValue) {
        println("Succesfully defined ${key.key} (arity: $arity, descriptor: $descriptor) to point to $function")
    }

    open fun annotationInit() {
        println("Base ann init :(")
    }
}
