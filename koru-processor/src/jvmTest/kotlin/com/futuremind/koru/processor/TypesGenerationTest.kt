package com.futuremind.koru.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.reflect.KVisibility

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
                            
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            interface InterfaceGenerationExample {
                                val someVal : Float
                                val someValFlow : Flow<Float>
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
        generatedType.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }


    @Test
    fun `should generate interface from class via @ToNativeInterface`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            class InterfaceGenerationExample {
                                val someVal : Float = TODO()
                                val someValFlow : Flow<Float> = TODO()
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
        generatedType.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate class from interface via @ToNativeClass`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "interface4.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            interface ClassGenerationExample {
                                val someVal : Float
                                val someValFlow : Flow<Float>
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
        generatedType.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate class from class via @ToNativeClass`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "class5.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            class ClassGenerationExample {
                                val someVal : Float = TODO()
                                val someValFlow : Flow<Float> = TODO()
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
        generatedType.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should generate interface and a class extending it, when annotating same class with both @ToNativeClass and @ToNativeInterface`() {

        val compilationResult = prepareCompilation(
            sourceFile = SourceFile.kotlin(
                "interface2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            @ToNativeInterface
                            class Example {
                                val someVal : Float = TODO()
                                val someValFlow : Flow<Float> = TODO()
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            tempDir = tempDir
        ).compile()

        val generatedInterface =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultInterfaceNameSuffix").kotlin
        val generatedClass =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultClassNameSuffix").kotlin

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        generatedInterface.java.isInterface shouldBe true
        generatedInterface.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedInterface.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedInterface.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedInterface.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedInterface.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"

        generatedClass.java.isInterface shouldBe false
        generatedClass.supertypes.map { it.toString() } shouldContain "com.futuremind.kmm101.test.Example$defaultInterfaceNameSuffix"
        generatedClass.memberReturnType("someVal") shouldBe "kotlin.Float"
        generatedClass.memberReturnType("someValFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedClass.memberReturnType("blocking") shouldBe "kotlin.Float"
        generatedClass.memberReturnType("suspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedClass.memberReturnType("flow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
    }

    @Test
    fun `should match generated class with generated interface if they matched in original code`() {

        val compilationResult = prepareCompilation(
            sourceFiles = listOf(
                SourceFile.kotlin(
                    "interface3.kt",
                    """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            interface IExample {
                                val someVal : Float
                                val someValFlow : Flow<Float>
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                        """
                ),
                SourceFile.kotlin(
                    "class3.kt",
                    """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            class Example : IExample {
                                override val someVal : Float = TODO()
                                override val someValFlow : Flow<Float> = TODO()
                                override fun blocking(whatever: Int) : Float = TODO()
                                override suspend fun suspending(whatever: Int) : Float = TODO()
                                override fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
                )
            ),
            tempDir = tempDir
        ).compile()

        val generatedInterface =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.IExample$defaultInterfaceNameSuffix").kotlin
        val generatedClass =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.Example$defaultClassNameSuffix").kotlin

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
                            
                            import com.futuremind.koru.ToNativeInterface
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
                            
                            import com.futuremind.koru.ToNativeClass
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

    @Test
    fun `should not wrap private members when generating class`() {

        compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "privateClass.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeClass
                            class PrivateFunctionsExample {
                                val someVal : Float = TODO()
                                val someValFlow : Flow<Float> = TODO()
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                                private val someValPrivate : Float = TODO()
                                private val someValFlowPrivate : Flow<Float> = TODO()
                                private fun blockingPrivate(whatever: Int) : Float = TODO()
                                private suspend fun suspendingPrivate(whatever: Int) : Float = TODO()
                                private fun flowPrivate(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.PrivateFunctionsExample$defaultClassNameSuffix",
            tempDir = tempDir
        )
        //enough to check it compiles, it would not with wrapped private function

    }

    @Test
    fun `should not wrap private functions when generating interface`() {

        compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "privateInterface.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            @ToNativeInterface
                            class PrivateFunctionsExample {
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                                private fun blockingPrivate(whatever: Int) : Float = TODO()
                                private suspend fun suspendingPrivate(whatever: Int) : Float = TODO()
                                private fun flowPrivate(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.PrivateFunctionsExample$defaultInterfaceNameSuffix",
            tempDir = tempDir
        )
        //enough to check it compiles, it would not with wrapped private function

    }

    @Test
    fun `should not extend superinterface on class if it is not annotated (and thus should strip override annotation)`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "notAnnotatedSuperInterface.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            //adding generic type to spice things up ;)
                            interface NotAnnotatedInterface<T: Any> {
                                val notAnnotatedVal : T
                                val notAnnotatedFlowVal : Flow<T>
                                fun notAnnotatedBlocking(whatever: Int) : T = TODO()
                                suspend fun notAnnotatedSuspending(whatever: Int) : T = TODO()
                                fun notAnnotatedFlow(whatever: Int) : Flow<T> = TODO()
                            }
                            
                            @ToNativeInterface
                            interface AnnotatedInterface {
                                val annotatedVal : Float
                                val annotatedFlowVal : Flow<Float>
                                fun annotatedBlocking(whatever: Int) : Float = TODO()
                                suspend fun annotatedSuspending(whatever: Int) : Float = TODO()
                                fun annotatedFlow(whatever: Int) : Flow<Float> = TODO()
                            }
                            
                            @ToNativeClass
                            class SuperInterfacesExample : NotAnnotatedInterface<Float>, AnnotatedInterface {
                                override val notAnnotatedVal : Float = TODO()
                                override val notAnnotatedFlowVal : Flow<Float> = TODO()
                                override fun notAnnotatedBlocking(whatever: Int) : Float = TODO()
                                override suspend fun notAnnotatedSuspending(whatever: Int) : Float = TODO()
                                override fun notAnnotatedFlow(whatever: Int) : Flow<Float> = TODO()

                                override val annotatedVal : Float = TODO()
                                override val annotatedFlowVal : Flow<Float> = TODO()
                                override fun annotatedBlocking(whatever: Int) : Float = TODO()
                                override suspend fun annotatedSuspending(whatever: Int) : Float = TODO()
                                override fun annotatedFlow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.SuperInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldNotContain "com.futuremind.kmm101.test.NotAnnotatedInterface$defaultInterfaceNameSuffix"
        generatedType.supertypes.map { it.toString() } shouldContain "com.futuremind.kmm101.test.AnnotatedInterface$defaultInterfaceNameSuffix"

        generatedType.memberReturnType("notAnnotatedVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("notAnnotatedFlowVal") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("notAnnotatedBlocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("notAnnotatedSuspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("notAnnotatedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"

        generatedType.memberReturnType("annotatedVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("annotatedFlowVal") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("annotatedBlocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("annotatedSuspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("annotatedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"

    }

    @Test
    fun `should not extend superinterface on interface if it is not annotated (and thus should strip override annotation)`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "notAnnotatedSuperInterface2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow

                            interface NotAnnotatedInterface<T: Any> {
                                val notAnnotatedVal : T
                                val notAnnotatedFlowVal : Flow<T>
                                fun notAnnotatedBlocking(whatever: Int) : T = TODO()
                                suspend fun notAnnotatedSuspending(whatever: Int) : T = TODO()
                                fun notAnnotatedFlow(whatever: Int) : Flow<T> = TODO()
                            }
                            
                            @ToNativeInterface
                            interface AnnotatedInterface {
                                val annotatedVal : Float
                                val annotatedFlowVal : Flow<Float>
                                fun annotatedBlocking(whatever: Int) : Float = TODO()
                                suspend fun annotatedSuspending(whatever: Int) : Float = TODO()
                                fun annotatedFlow(whatever: Int) : Flow<Float> = TODO()
                            }
                            
                            @ToNativeInterface
                            class SuperInterfacesExample : NotAnnotatedInterface<Float>, AnnotatedInterface {
                                override val notAnnotatedVal : Float = TODO()
                                override val notAnnotatedFlowVal : Flow<Float> = TODO()
                                override fun notAnnotatedBlocking(whatever: Int) : Float = TODO()
                                override suspend fun notAnnotatedSuspending(whatever: Int) : Float = TODO()
                                override fun notAnnotatedFlow(whatever: Int) : Flow<Float> = TODO()

                                override val annotatedVal : Float = TODO()
                                override val annotatedFlowVal : Flow<Float> = TODO()
                                override fun annotatedBlocking(whatever: Int) : Float = TODO()
                                override suspend fun annotatedSuspending(whatever: Int) : Float = TODO()
                                override fun annotatedFlow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.SuperInterfacesExample$defaultInterfaceNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldNotContain "com.futuremind.kmm101.test.NotAnnotatedInterface$defaultInterfaceNameSuffix"
        generatedType.supertypes.map { it.toString() } shouldContain "com.futuremind.kmm101.test.AnnotatedInterface$defaultInterfaceNameSuffix"

        generatedType.memberReturnType("notAnnotatedVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("notAnnotatedFlowVal") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("notAnnotatedBlocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("notAnnotatedSuspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("notAnnotatedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"

        generatedType.memberReturnType("annotatedVal") shouldBe "kotlin.Float"
        generatedType.memberReturnType("annotatedFlowVal") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"
        generatedType.memberReturnType("annotatedBlocking") shouldBe "kotlin.Float"
        generatedType.memberReturnType("annotatedSuspending") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("annotatedFlow") shouldBe "com.futuremind.koru.FlowWrapper<kotlin.Float>"

    }

    @Test
    fun `should extend multiple @ToNativeInterface superinterfaces (on standalone annotated interfaces)`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "multipleSuperInterfaces.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface FirstInterface {
                                suspend fun firstFunction(whatever: Int) : Float = TODO()
                            }

                            @ToNativeInterface
                            interface SecondInterface {
                                suspend fun secondFunction(whatever: Int) : Float = TODO()
                            }

                            @ToNativeInterface
                            interface ThirdInterface {
                                suspend fun thirdFunction(whatever: Int) : Float = TODO()
                            }
                            
                            @ToNativeClass
                            class MultipleInterfacesExample : FirstInterface, SecondInterface, ThirdInterface {
                                override suspend fun firstFunction(whatever: Int) : Float = TODO()
                                override suspend fun secondFunction(whatever: Int) : Float = TODO()
                                override suspend fun thirdFunction(whatever: Int) : Float = TODO()
                                suspend fun fourthFunction(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldContainAll listOf(
            "com.futuremind.kmm101.test.FirstInterface$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.SecondInterface$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.ThirdInterface$defaultInterfaceNameSuffix",
        )

        generatedType.memberReturnType("firstFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("secondFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("thirdFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("fourthFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"

    }

    @Test
    fun `should extend multiple @ToNativeInterface superinterfaces including the one annotated directly on class`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "multipleSuperInterfaces2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface FirstInterface {
                                suspend fun firstFunction(whatever: Int) : Float = TODO()
                            }

                            @ToNativeInterface
                            interface SecondInterface {
                                suspend fun secondFunction(whatever: Int) : Float = TODO()
                            }
                            
                            @ToNativeClass
                            @ToNativeInterface
                            class MultipleInterfacesExample : FirstInterface, SecondInterface {
                                override suspend fun firstFunction(whatever: Int) : Float = TODO()
                                override suspend fun secondFunction(whatever: Int) : Float = TODO()
                                suspend fun thirdFunction(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldContainAll listOf(
            "com.futuremind.kmm101.test.FirstInterface$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.SecondInterface$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultInterfaceNameSuffix",
        )

    }

    @Test
    fun `should generate complex inheritance hierarchy`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "multipleSuperInterfaces3.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface A {
                                suspend fun a(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface B {
                                suspend fun b(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface C : B {
                                suspend fun c(whatever: Int) : Float
                                override suspend fun b(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface D : C, A {
                                suspend fun d(whatever: Int) : Float
                                override suspend fun a(whatever: Int) : Float
                                override suspend fun b(whatever: Int) : Float
                                override suspend fun c(whatever: Int) : Float
                            }

                            interface Z {
                                suspend fun z(whatever: Int) : Float
                            }
                            
                            @ToNativeClass
                            @ToNativeInterface
                            class MultipleInterfacesExample : A, D, Z {
                                override suspend fun a(whatever: Int) : Float = TODO()
                                override suspend fun b(whatever: Int) : Float = TODO()
                                override suspend fun c(whatever: Int) : Float = TODO()
                                override suspend fun d(whatever: Int) : Float = TODO()
                                suspend fun e(whatever: Int) : Float = TODO()
                                override suspend fun z(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldContainAll listOf(
            "com.futuremind.kmm101.test.A$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.D$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultInterfaceNameSuffix",
        )

        //Z is inherited directly but not annotated
        //B and C are not inherited directly, their methods will be generated but superinterfaces will not contain them explicitly (TODO test if that's true)
        generatedType.supertypes.map { it.toString() } shouldNotContainAnyOf listOf(
            "com.futuremind.kmm101.test.B$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.C$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.Z$defaultInterfaceNameSuffix",
        )

    }

    @Test
    fun `should generate complex inheritance hierarchy with intermediate unannotated interface (Y)`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "multipleSuperInterfaces4.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface A : Y {
                                suspend fun a(whatever: Int) : Float
                                override suspend fun y(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface B {
                                suspend fun b(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface C : B {
                                suspend fun c(whatever: Int) : Float
                                override suspend fun b(whatever: Int) : Float
                            }

                            @ToNativeInterface
                            interface D : C, A {
                                suspend fun d(whatever: Int) : Float
                                override suspend fun a(whatever: Int) : Float
                                override suspend fun b(whatever: Int) : Float
                                override suspend fun c(whatever: Int) : Float
                                override suspend fun y(whatever: Int) : Float
                            }

                            interface Y {
                                suspend fun y(whatever: Int) : Float
                            }

                            interface Z {
                                suspend fun z(whatever: Int) : Float
                            }
                            
                            @ToNativeClass
                            @ToNativeInterface
                            class MultipleInterfacesExample : A, D, Z, Y {
                                override suspend fun a(whatever: Int) : Float = TODO()
                                override suspend fun b(whatever: Int) : Float = TODO()
                                override suspend fun c(whatever: Int) : Float = TODO()
                                override suspend fun d(whatever: Int) : Float = TODO()
                                suspend fun e(whatever: Int) : Float = TODO()
                                override suspend fun y(whatever: Int) : Float = TODO()
                                override suspend fun z(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldContainAll listOf(
            "com.futuremind.kmm101.test.A$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.D$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultInterfaceNameSuffix",
        )

        //Z is inherited directly but not annotated
        //B&C are not inherited directly
        //Y is unannotated, but inherited indirectly via A
        generatedType.supertypes.map { it.toString() } shouldNotContainAnyOf listOf(
            "com.futuremind.kmm101.test.B$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.C$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.Z$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.Y$defaultInterfaceNameSuffix",
        )

    }

    @Test
    fun `should not extend a foreign generated interface (one that has not been a superinterface of the original class)`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "multipleSuperInterfaces2.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface FirstInterface {
                                suspend fun firstFunction(whatever: Int) : Float = TODO()
                            }

                            @ToNativeInterface
                            interface SecondInterface {
                                suspend fun secondFunction(whatever: Int) : Float = TODO()
                            }

                            @ToNativeInterface
                            interface ThirdInterface {
                                suspend fun thirdFunction(whatever: Int) : Float = TODO()
                            }
                            
                            @ToNativeClass
                            class MultipleInterfacesExample : FirstInterface, SecondInterface {
                                override suspend fun firstFunction(whatever: Int) : Float = TODO()
                                override suspend fun secondFunction(whatever: Int) : Float = TODO()
                                suspend fun fourthFunction(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )

        generatedType.supertypes.map { it.toString() } shouldContainAll listOf(
            "com.futuremind.kmm101.test.FirstInterface$defaultInterfaceNameSuffix",
            "com.futuremind.kmm101.test.SecondInterface$defaultInterfaceNameSuffix",
        )

        generatedType.supertypes.map { it.toString() } shouldNotContain "com.futuremind.kmm101.test.ThirdInterface$defaultInterfaceNameSuffix"

        generatedType.memberReturnType("firstFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("secondFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"
        generatedType.memberReturnType("fourthFunction") shouldBe "com.futuremind.koru.SuspendWrapper<kotlin.Float>"

    }

    @Test
    fun `should not add override modifier from a foreign interface just because the name matches`() {

        val generatedType = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "matchingFunctionNamesFalsePositive.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface
                            import kotlinx.coroutines.flow.Flow
                            
                            @ToNativeInterface
                            interface FirstInterface {
                                val someVal : Float
                                val someValFlow : Flow<Float>
                                fun blocking(whatever: Int) : Float
                                suspend fun suspending(whatever: Int) : Float
                                fun flow(whatever: Int) : Flow<Float>
                            }
                            
                            @ToNativeClass
                            class MatchingNamesExample {
                                val someVal : Float = TODO()
                                val someValFlow : Flow<Float> = TODO()
                                fun blocking(whatever: Int) : Float = TODO()
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MatchingNamesExample$defaultClassNameSuffix",
            tempDir = tempDir
        )
        //just checking that it compiles is enough, would not compile with override pointing to missing interface
    }

    @Test
    fun `should keep internal visibility when generating class or add public when omitted`() {

        val compilationResult = prepareCompilation(
            sourceFile = SourceFile.kotlin(
                "visbility.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.koru.ToNativeClass
                            import com.futuremind.koru.ToNativeInterface

                            @ToNativeClass
                            @ToNativeInterface
                            internal class InternalExample {
                                suspend fun suspending(whatever: Int) : Float = TODO()
                            }

                            @ToNativeClass
                            @ToNativeInterface
                            class DefaultExample {
                                suspend fun suspending(whatever: Int) : Float = TODO()
                            }
                        """
            ),
            tempDir = tempDir
        ).compile()

        val internalInterface =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.InternalExample$defaultInterfaceNameSuffix").kotlin
        val internalClass =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.InternalExample$defaultClassNameSuffix").kotlin

        val defaultInterface =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.DefaultExample$defaultInterfaceNameSuffix").kotlin
        val defaultClass =
            compilationResult.classLoader.loadClass("com.futuremind.kmm101.test.DefaultExample$defaultClassNameSuffix").kotlin

        internalInterface.visibility shouldBe KVisibility.INTERNAL
        internalClass.visibility shouldBe KVisibility.INTERNAL

        defaultInterface.visibility shouldBe KVisibility.PUBLIC
        defaultClass.visibility shouldBe KVisibility.PUBLIC

    }

    @Test
    fun `should throw on interface generation from private type`() = testThrowsCompilationError(
        source = SourceFile.kotlin(
            "private.kt",
            """
                        package com.futuremind.kmm101.test
                        
                        import com.futuremind.koru.ToNativeInterface

                        @ToNativeInterface
                        private class PrivateClassExample {
                            suspend fun suspending(whatever: Int) : Float = TODO()
                        }
                        """
        ),
        expectedMessage = "Cannot wrap types with `private` modifier. Consider using internal or public.",
        tempDir = tempDir
    )

    @Test
    fun `should throw on class generation from private type`() = testThrowsCompilationError(
        source = SourceFile.kotlin(
            "private.kt",
            """
                        package com.futuremind.kmm101.test
                        
                        import com.futuremind.koru.ToNativeClass

                        @ToNativeClass
                        private class PrivateClassExample {
                            suspend fun suspending(whatever: Int) : Float = TODO()
                        }
                        """
        ),
        expectedMessage = "Cannot wrap types with `private` modifier. Consider using internal or public.",
        tempDir = tempDir
    )

    @Test
    fun `should freeze wrapper if freeze=true in annotation`() {

        val classToWrap = SourceFile.kotlin(
            "freeze1.kt",
            """
                        package com.futuremind.kmm101.test
                        
                        import com.futuremind.koru.ToNativeClass
                        import kotlinx.coroutines.flow.Flow
                        
                            @ToNativeClass(freeze = true)
                            class FreezeExample {
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                    """
        )

        val compilationResult = prepareCompilation(
            sourceFiles = listOf(classToWrap),
            tempDir = tempDir
        ).compile()

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val generatedClass = compilationResult.generatedFiles
            .getContentByFilename("FreezeExample$defaultClassNameSuffix.kt")

        generatedClass shouldContain "FlowWrapper<Float> = FlowWrapper(scopeProvider, true"
        generatedClass shouldContain "SuspendWrapper(scopeProvider, true"
        generatedClass shouldContain "this.freeze()"
    }

    @Test
    fun `should not freeze wrapper by default`() {

        val classToWrap = SourceFile.kotlin(
            "freeze2.kt",
            """
                        package com.futuremind.kmm101.test
                        
                        import com.futuremind.koru.ToNativeClass
                        import kotlinx.coroutines.flow.Flow
                        
                            @ToNativeClass
                            class FreezeExample {
                                suspend fun suspending(whatever: Int) : Float = TODO()
                                fun flow(whatever: Int) : Flow<Float> = TODO()
                            }
                    """
        )

        val compilationResult = prepareCompilation(
            sourceFiles = listOf(classToWrap),
            tempDir = tempDir
        ).compile()

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val generatedClass = compilationResult.generatedFiles
            .getContentByFilename("FreezeExample$defaultClassNameSuffix.kt")

        generatedClass shouldContain "FlowWrapper<Float> = FlowWrapper(scopeProvider, false"
        generatedClass shouldContain "SuspendWrapper(scopeProvider, false"
        generatedClass shouldNotContain "this.freeze()"
    }

}