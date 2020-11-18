package me.mattco.reeva.runtime.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JSNativeAccessorGetter(
    val name: String,
    val attributes: Int = 3
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JSNativeAccessorSetter(
    val name: String,
    val attributes: Int = 3
)
