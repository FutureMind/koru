package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InterfaceGenerationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should generate interface from interface via @ToNativeInterface`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            interface InterfaceGenerationExample {
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.InterfaceGenerationExample$defaultInterfaceNameSuffix",
            tempDir = tempDir
        )

        generatedType.java.isInterface shouldBe true
        generatedType.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedType.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate interface from class via @ToNativeInterface`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            class InterfaceGenerationExample {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.InterfaceGenerationExample$defaultInterfaceNameSuffix",
            tempDir = tempDir
        )

        generatedType.java.isInterface shouldBe true
        generatedType.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedType.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
    }

}