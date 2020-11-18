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
    fun simpleVarDeclaration() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import kotlin.test.assertEquals
            fun main() {
              val myVarName = UIBlock()
              assertEquals("myVarName", myVarName.componentName)
            }"""
        )
    }

    @Test
    fun complexVarDeclaration() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
              val myVarName = UIBlock()
              val myComplexVar = (UIBlock().constrain {  } childOf myVarName).apply {} as UIComponent
              assertEquals("myVarName", myVarName.componentName)
              assertEquals("myComplexVar", myComplexVar.componentName)
            }"""
        )
    }

    @Test
    fun simpleVarAssignment() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
              val myVarName: UIComponent
              myVarName = UIBlock()
              assertEquals("myVarName", myVarName.componentName)
            }"""
        )
    }

    @Test
    fun complexVarAssignment() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
              val myVarName: UIComponent
              myVarName = UIBlock()
              val myComplexVar: UIComponent
              myComplexVar = (UIBlock().constrain {  } childOf myVarName).apply {} as UIComponent
              assertEquals("myVarName", myVarName.componentName)
              assertEquals("myComplexVar", myComplexVar.componentName)
            }"""
        )
    }

    @Test
    fun simpleFieldDeclaration() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
                Test()
            }
            class Test {
                private val myFieldName = UIBlock()
                
                init {
                    println(this)
                    assertEquals("myFieldName", myFieldName.componentName)
                }
            }"""
        )
    }

    @Test
    fun complexFieldDeclaration() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
                Test()
            }
            class Test {
                private val myFieldName = UIBlock()
                private val myComplexField = (UIBlock().constrain {  } childOf myFieldName).apply {} as UIComponent
                
                init {
                    assertEquals("myFieldName", myFieldName.componentName)
                    assertEquals("myComplexField", myComplexField.componentName)
                }
            }
            """
        )
    }

    @Test
    fun simpleFieldAssignment() {
        compiledTestHelper(
            """
            import club.sk1er.elementa.UIBlock
            import club.sk1er.elementa.UIComponent
            import kotlin.test.assertEquals
            fun main() {
                Test()
            }
            class Test {
                private val myFieldName: UIComponent
                
                init {
                    myFieldName = UIBlock()
                    assertEquals("myFieldName", myFieldName.componentName)
                }
            }"""
        )
    }

    @Test
    fun complexFieldAssignment() {
        compiledTestHelper(
            """
            import me.mattco.reeva.runtime.annotations.JSMethod
            import me.mattco.reeva.runtime.objects.JSObject
            import me.mattco.reeva.runtime.JSValue
            import me.mattco.reeva.runtime.objects.PropertyKey
            
            class JSThing : JSObject() {
            @JSMethod("someMethod", 3, 0b101)
            fun jsMethod(thisValue: JSValue, args: List<JSValue>): JSValue { return this }
            
            fun xxx() {
                defineNativeFunction(PropertyKey("someMethod"), 3, 5, ::jsMethod)
            }
            }
            
            fun main() {
                val x = { y: List<JSValue> -> JSThing() }
                val z = JSThing::jsMethod
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
