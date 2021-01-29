package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TypesGenerationTest {

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
                "interface2.kt",
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

    @Test
    fun `should generate class from interface via @ToNativeClass`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface4.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            interface ClassGenerationExample {
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.ClassGenerationExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.java.isInterface shouldBe false
        generatedType.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedType.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate class from class via @ToNativeClass`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "class5.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            class ClassGenerationExample {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.ClassGenerationExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.java.isInterface shouldBe false
        generatedType.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedType.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate interface and a class extending it, when annotating same class with both @ToNativeClass and @ToNativeInterface`() {

        val compilationResult = prepareCompilation(
            sourceFile = SourceFile.kotlin(
                "interface2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            @ToNativeInterface
                            class Example {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            tempDir = tempDir
        ).compile()

        val generatedInterface = compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultInterfaceNameSuffix").kotlin
        val generatedClass = compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultClassNameSuffix").kotlin

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        generatedInterface.java.isInterface shouldBe true
        generatedInterface.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedInterface.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedInterface.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"

        generatedClass.java.isInterface shouldBe false
        generatedClass.supertypes.map { it.toString() } shouldContain "com.futuremind.kmm101.test.Example$defaultInterfaceNameSuffix"
        generatedClass.methodReturnType("blocking") shouldBe "kotlin.Float"
        generatedClass.methodReturnType("suspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Float>"
        generatedClass.methodReturnType("flow") shouldBe "com.futuremind.iossuspendwrapper.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun`should match generated class with generated interface if they matched in original code`(){

        val compilationResult = prepareCompilation(
            sourceFiles = listOf(
                SourceFile.kotlin(
                    "interface3.kt",
                    """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            interface IExample {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
                ),
                SourceFile.kotlin(
                    "class3.kt",
                    """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            class Example : IExample {
                                override fun blocking(whatever: Int) : Float = TODO()
                                override suspend fun suspending(whatever: Int) : Float = TODO()
                                override fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
                )
            ),
            tempDir = tempDir
        ).compile()

        val generatedInterface = compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.IExample$defaultInterfaceNameSuffix").kotlin
        val generatedClass = compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultClassNameSuffix").kotlin

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generatedInterface.java.isInterface shouldBe true
        generatedClass.java.isInterface shouldBe false
        generatedClass.supertypes.map { it.toString() } shouldContain "com.futuremind.kmm101.test.IExample$defaultInterfaceNameSuffix"

    }

    @Test
    fun `should generate interface with custom name via @ToNativeInterface(name)`() {

        compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface6.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface(name="CustomIosProtocol")
                            interface InterfaceGenerationExample {
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.CustomIosProtocol",
            tempDir = tempDir
        )
    }

    @Test
    fun `should generate class with custom name via @ToNativeClass(name)`() {

        compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "class6.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass(name="CustomIos")
                            interface InterfaceGenerationExample {
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.CustomIos",
            tempDir = tempDir
        )
    }

    //TODO vals in interfaces are not handled properly yet
    @Test
    fun `should properly copy all superinterfaces unrelated to our wrappers`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "class5.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            import com.futuremind.iossuspendwrapper.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            interface UnrelatedInterface {
                                fun doWhatever() : String
                            }
                            
                            @ToNativeInterface
                            interface RelatedInterface {
                                suspend fun doWhateverSuspending() : String
                            }
                            
                            @ToNativeClass
                            class SuperInterfacesExample : UnrelatedInterface, RelatedInterface {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                                override fun doWhatever(): String = TODO()
                                override suspend fun doWhateverSuspending(): String = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.SuperInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.methodReturnType("doWhatever") shouldBe "kotlin.String"
        generatedType.methodReturnType("doWhateverSuspending") shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.String>"
    }

}