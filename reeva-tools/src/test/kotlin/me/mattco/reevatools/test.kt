package me.mattco.reevatools

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.fail

class CompilerTest {
    @Test
    fun `@JSMethod generation`() {
        compiledTestHelper(
            """
            import me.mattco.reeva.runtime.annotations.JSMethod
            import me.mattco.reeva.runtime.objects.JSObject
            import me.mattco.reeva.runtime.JSValue
            import me.mattco.reeva.runtime.objects.PropertyKey

            class JSThing : JSObject() {
                @JSMethod("toStringTag", 3, "CeW")
                fun jsMethodWithStrName(thisValue: JSValue, args: List<JSValue>): JSValue {
                    return this
                }

                @JSMethod("@@toStringTag", 3)
                fun jsMethodWithSymbolName(thisValue: JSValue, args: List<JSValue>): JSValue {
                    return this
                }
            }

            fun main() {
                JSThing().annotationInit()
            }
            """
        )
    }

    @Test
    fun `@JSNativeProperty{Getter,Setter} generation`() {
        compiledTestHelper(
            """
            import me.mattco.reeva.runtime.annotations.JSNativePropertyGetter
            import me.mattco.reeva.runtime.annotations.JSNativePropertySetter
            import me.mattco.reeva.runtime.objects.JSObject
            import me.mattco.reeva.runtime.JSValue
            import me.mattco.reeva.runtime.objects.PropertyKey

            class JSThing : JSObject() {
                @JSNativePropertyGetter("length", "E")
                fun getLength(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativePropertySetter("length", "cew")
                fun setLength(thisValue: JSValue, newValue: JSValue) {

                }
                @JSNativePropertyGetter("@@toStringTag", "E")
                fun `get@@toStringTag`(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativePropertySetter("@@toStringTag", "cew")
                fun `set@@toStringTag`(thisValue: JSValue, newValue: JSValue) {

                }
            }

            fun main() {
                JSThing().annotationInit()
            }
            """
        )
    }

    @Test
    fun `@JSNativeAccessor{Getter,Setter} generation`() {
        compiledTestHelper(
            """
            import me.mattco.reeva.runtime.annotations.JSNativeAccessorGetter
            import me.mattco.reeva.runtime.annotations.JSNativeAccessorSetter
            import me.mattco.reeva.runtime.objects.JSObject
            import me.mattco.reeva.runtime.JSValue
            import me.mattco.reeva.runtime.objects.PropertyKey

            class JSThing : JSObject() {
                @JSNativeAccessorGetter("length", "E")
                fun getLength(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativeAccessorSetter("length", "cew")
                fun setLength(thisValue: JSValue, newValue: JSValue) {

                }

                @JSNativeAccessorGetter("@@toStringTag", "E")
                fun `get@@toStringTag`(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativeAccessorSetter("@@toStringTag", "cew")
                fun `set@@toStringTag`(thisValue: JSValue, newValue: JSValue) {

                }
            }

            fun main() {
                JSThing().annotationInit()
            }
            """
        )
    }

    @Test
    fun `non-constant attributes`() {
        compiledTestHelper(
            """
            import me.mattco.reeva.runtime.annotations.JSMethod
            import me.mattco.reeva.runtime.annotations.JSNativePropertyGetter
            import me.mattco.reeva.runtime.annotations.JSNativePropertySetter
            import me.mattco.reeva.runtime.annotations.JSNativeAccessorGetter
            import me.mattco.reeva.runtime.annotations.JSNativeAccessorSetter
            import me.mattco.reeva.runtime.objects.JSObject
            import me.mattco.reeva.runtime.JSValue
            import me.mattco.reeva.runtime.objects.PropertyKey

            class JSThing : JSObject() {
                @JSMethod("a", 3, JSObject.ATTRIBUTE)
                fun jsMethodWithStrName(thisValue: JSValue, args: List<JSValue>): JSValue {
                    return this
                }

                @JSNativePropertyGetter("b", JSObject.ATTRIBUTE)
                fun getB(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativePropertySetter("b", JSObject.ATTRIBUTE)
                fun setB(thisValue: JSValue, newValue: JSValue) {

                }

                @JSNativeAccessorGetter("c", JSObject.ATTRIBUTE)
                fun getC(thisValue: JSValue): JSValue {
                    return this
                }

                @JSNativeAccessorSetter("c", JSObject.ATTRIBUTE)
                fun setC(thisValue: JSValue, newValue: JSValue) {

                }
            }

            fun main() {
                JSThing().annotationInit()
            }
            """
        )
    }
}

fun compiledTestHelper(@Language("kotlin") source: String) {
    val result = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("main.kt", source))
        useIR = true
        messageOutputStream = System.out
        compilerPlugins = listOf(ReevaToolsComponentRegistrar())
        inheritClassPath = true
    }.compile()

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
//    result.outputDirectory.copyRecursively(File("classes"))

    val kClazz = result.classLoader.loadClass("MainKt")
    val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
    try {
        try {
            main.invoke(null)
        } catch (t: InvocationTargetException) {
            throw t.cause!!
        }
    } catch (t: Throwable) {
        fail("Shouldn't have thrown exception", t)
    }
}
