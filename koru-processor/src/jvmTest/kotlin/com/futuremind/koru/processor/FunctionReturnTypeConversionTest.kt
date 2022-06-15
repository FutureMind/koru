package com.futuremind.koru.processor

import com.futuremind.koru.processor.utils.*
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import java.io.File


class FunctionReturnTypeConversionTest {

    @TempDir
    lateinit var tempDir: File

    @ProcessorTest
    fun `should generate wrappers for suspend functions`(processorType: ProcessorType) {

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
            tempDir = tempDir,
            processorType = processorType
        )

        generatedClass.memberReturnType("doSth") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Unit>"
        generatedClass.memberReturnType("returnSthSimple") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedClass.memberReturnType("returnSthComplex") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
    }

    @ProcessorTest
    fun `should generate wrappers for blocking functions`(processorType: ProcessorType) {

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
            tempDir = tempDir,
            processorType = processorType
        )

        generatedClass.memberReturnType("doSth") shouldBe "kotlin.Unit"
        generatedClass.memberReturnType("returnSthSimple") shouldBe "kotlin.Float"
        generatedClass.memberReturnType("returnSthComplex") shouldBe "kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>"
    }

    @ProcessorTest
    fun `should generate wrappers for Flow returning functions`(processorType: ProcessorType) {

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
            tempDir = tempDir,
            processorType = processorType
        )

        generatedClass.memberReturnType("doSth") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Unit>"
        generatedClass.memberReturnType("returnSthSimple") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("returnSthComplex") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
    }

    @ProcessorTest
    fun `should generate wrappers for SharedFlow, StateFlow etc returning functions`(processorType: ProcessorType) {

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
            tempDir = tempDir,
            processorType = processorType
        )

        generatedClass.memberReturnType("stateFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("sharedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("mutableStateFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("mutableSharedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

    @ProcessorTest
    fun `should generate wrappers for properties`(processorType: ProcessorType) {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "props1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            interface Whatever

                            @ToNativeClass
                            class PropertiesExample {
                                val someVal : Float = TODO()
                                var someVar : Float = TODO()
                                val someValComplex : List<Map<Int, Whatever>> = TODO()
                                val someValFlow : Flow<Float> = TODO()
                                var someVarFlow : Flow<Float> = TODO()
                                val someValFlowComplex : Flow<List<Map<Int, Whatever>>> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.PropertiesExample$defaultClassNameSuffix",
            tempDir = tempDir,
            processorType = processorType
        )

        generatedClass.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedClass.memberReturnType("someVar") shouldBe "kotlin.Float"
        generatedClass.memberReturnType("someValComplex") shouldBe "kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>"
        generatedClass.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("someVarFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("someValFlowComplex") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
    }

}