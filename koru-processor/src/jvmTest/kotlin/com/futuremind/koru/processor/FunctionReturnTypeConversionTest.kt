package com.futuremind.koru.processor

import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File


class FunctionReturnTypeConversionTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should generate wrappers for suspend functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "suspend1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass

                            interface Whatever

                            @ToNativeClass
                            class SuspendExample {
                                suspend fun doSth(whatever: Int) {  }
                                suspend fun returnSthSimple(whatever: Int) : Float = TODO()
                                suspend fun returnSthComplex(whatever: Int) : List<Map<Int, Whatever>> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.SuspendExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedClass.methodReturnType("doSth") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Unit>"
        generatedClass.methodReturnType("returnSthSimple") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedClass.methodReturnType("returnSthComplex") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
    }

    @Test
    fun `should generate wrappers for blocking functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "blocking1.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.koru.ToNativeClass
                
                interface Whatever

                @ToNativeClass
                class BlockingExample {
                    fun doSth(whatever: Int){ }
                    fun returnSthSimple(whatever: Int) : Float = TODO()
                    fun returnSthComplex(whatever: Int) : List<Map<Int, Whatever>> = TODO()
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.BlockingExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedClass.methodReturnType("doSth") shouldBe "kotlin.Unit"
        generatedClass.methodReturnType("returnSthSimple") shouldBe "kotlin.Float"
        generatedClass.methodReturnType("returnSthComplex") shouldBe "kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>"
    }

    @Test
    fun `should generate wrappers for Flow returning functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "flow1.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.koru.ToNativeClass
                import kotlinx.coroutines.flow.Flow
                
                interface Whatever

                @ToNativeClass
                class FlowExample {
                    fun doSth(whatever: Int) : Flow<Unit> = TODO()
                    fun returnSthSimple(whatever: Int) : Flow<Float> = TODO()
                    fun returnSthComplex(whatever: Int) : Flow<List<Map<Int, Whatever>>> = TODO()
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.FlowExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedClass.methodReturnType("doSth") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Unit>"
        generatedClass.methodReturnType("returnSthSimple") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.methodReturnType("returnSthComplex") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
    }

    @Test
    fun `should generate wrappers for SharedFlow, StateFlow etc returning functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "flow2.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.koru.ToNativeClass
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.MutableStateFlow
                import kotlinx.coroutines.flow.MutableSharedFlow
                import kotlinx.coroutines.flow.SharedFlow
                import kotlinx.coroutines.flow.StateFlow

                @ToNativeClass
                class VariousFlowExample {
                    fun stateFlow(whatever: Int) : StateFlow<Float> = TODO()
                    fun sharedFlow(whatever: Int) : SharedFlow<Float> = TODO()
                    fun mutableStateFlow(whatever: Int) : MutableStateFlow<Float> = TODO()
                    fun mutableSharedFlow(whatever: Int) : MutableSharedFlow<Float> = TODO()
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.VariousFlowExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedClass.methodReturnType("stateFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.methodReturnType("sharedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.methodReturnType("mutableStateFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.methodReturnType("mutableSharedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

}