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
                @JSMethod("@@toStringTag", 3, 0b101)
                fun jsMethod(thisValue: JSValue, args: List<JSValue>): JSValue { 
                    return this 
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
    result.outputDirectory.copyRecursively(File("classes"))

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
