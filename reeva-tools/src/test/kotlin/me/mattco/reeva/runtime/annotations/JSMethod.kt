package me.mattco.reeva.runtime.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JSMethod(
    val name: String,
    val length: Int,
    val attributes: Int = 0
)
