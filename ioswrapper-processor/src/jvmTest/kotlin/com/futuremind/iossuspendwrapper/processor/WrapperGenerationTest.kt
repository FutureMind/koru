package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File


class WrapperGenerationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should generate wrappers for suspend functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "suspend1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass

                            interface Whatever

                            @ToNativeClass
                            class SuspendExample {
                                suspend fun doSth(whatever: Int) {  }
                                suspend fun returnSthSimple(whatever: Int) : Float = TODO()
                                suspend fun returnSthComplex(whatever: Int) : List<Map<Int, Whatever>> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.SuspendExampleNative",
            tempDir = tempDir
        )

        with(generatedClass.members.find { it.name == "doSth" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Unit>"
        }
        with(generatedClass.members.find { it.name == "returnSthSimple" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        }
        with(generatedClass.members.find { it.name == "returnSthComplex" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
        }
    }

    @Test
    fun `should generate wrappers for blocking functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "blocking1.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ToNativeClass
                
                interface Whatever

                @ToNativeClass
                class BlockingExample {
                    fun doSth(whatever: Int){ }
                    fun returnSthSimple(whatever: Int) : Float = TODO()
                    fun returnSthComplex(whatever: Int) : List<Map<Int, Whatever>> = TODO()
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.BlockingExampleNative",
            tempDir = tempDir
        )

        with(generatedClass.members.find { it.name == "doSth" }!!) {
            returnType.toString() shouldBe "kotlin.Unit"
        }
        with(generatedClass.members.find { it.name == "returnSthSimple" }!!) {
            returnType.toString() shouldBe "kotlin.Float"
        }
        with(generatedClass.members.find { it.name == "returnSthComplex" }!!) {
            returnType.toString() shouldBe "kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>"
        }
    }

    @Test
    fun `should generate wrappers for Flow returning functions`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "flow1.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ToNativeClass
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
            generatedClassCanonicalName = "com.futuremind.kmm101.test.FlowExampleNative",
            tempDir = tempDir
        )

        with(generatedClass.members.find { it.name == "doSth" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Unit>"
        }
        with(generatedClass.members.find { it.name == "returnSthSimple" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
        }
        with(generatedClass.members.find { it.name == "returnSthComplex" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
        }
    }


}