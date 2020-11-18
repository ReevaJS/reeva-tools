package me.mattco.reeva.runtime.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JSNativePropertyGetter(
    val name: String,
    val attributes: Int = 3
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JSNativePropertySetter(
    val name: String,
    val attributes: Int = 3
)
