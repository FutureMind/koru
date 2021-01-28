package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.reflect.KClass


//TODO this is just a stub, more tests required
class WrappedClassCodegenTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should throw if @ExportedScopeProvider is applied to class which doesn't extend ScopeProvider`() =
        testThrowsCompilationError(
        source = SourceFile.kotlin(
            "scopeProvider1.kt",
            """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ExportedScopeProvider
                import com.futuremind.iossuspendwrapper.ScopeProvider
                import kotlinx.coroutines.MainScope
                
                @ExportedScopeProvider
                class MainScopeProvider {
                    override val scope = MainScope()
                }
            """
        ),
        expectedMessage = "ExportedScopeProvider can only be applied to a class extending ScopeProvider interface"
    )

    @Test
    fun `should generate top level property with scope provider`() {

        val source = SourceFile.kotlin(
            "scopeProvider2.kt",
            """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ExportedScopeProvider
                import com.futuremind.iossuspendwrapper.ScopeProvider
                import kotlinx.coroutines.MainScope
                
                @ExportedScopeProvider
                class MainScopeProvider : ScopeProvider {
                    override val scope = MainScope()
                }
            """
        )

        val compilationResult = prepareCompilation(source).compile()

        val generatedScopeProvider = compilationResult.generatedFiles
            .find { it.name == "MainScopeProviderContainer.kt" }!!
            .readText()
            .trim()

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generatedScopeProvider shouldBe
                """
                    |package com.futuremind.kmm101.test
                    |
                    |public val exportedScopeProvider_mainScopeProvider: MainScopeProvider = MainScopeProvider()
                """.trimMargin().trim()

    }

    @Test
    fun `should generate wrapper for a suspend function returning Unit`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "unit1.kt",
                """
                            package com.futuremind.kmm101.test
                            
                            import com.futuremind.iossuspendwrapper.ToNativeClass
                            
                            @ToNativeClass
                            class FireAndWait {
                                suspend fun doSthSuspending(whatever: Int){ }
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.FireAndWaitIos"
        )

        with(generatedClass.members.find { it.name == "doSthSuspending" }!!) {
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.Unit>"
        }
    }

    @Test
    fun `should generate wrapper for a blocking function returning Unit`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "unit2.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ToNativeClass
                
                @ToNativeClass
                class FireAndWait {
                    fun doSthBlocking(whatever: Int){ }
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.FireAndWaitIos"
        )

        with(generatedClass.members.find { it.name == "doSthBlocking" }!!) {
            returnType.toString() shouldBe "kotlin.Unit"
        }
    }

    @Test
    fun `should generate wrapper for a suspend function returning complex type`() {

        val generatedClass = compileAndReturnGeneratedClass(
            source = SourceFile.kotlin(
                "complexType1.kt",
                """
                package com.futuremind.kmm101.test
                
                import com.futuremind.iossuspendwrapper.ToNativeClass
                
                interface Whatever

                @ToNativeClass
                class LoadComplexTypeUseCase {
                    suspend fun loadComplex(whatever: Int) : List<Map<Int, Whatever>>{ 
                        return listOf()
                    }
                }
            """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.LoadComplexTypeUseCaseIos"
        )

        with(generatedClass.members.find { it.name == "loadComplex" }!!) {
            println("Type: $this")
            returnType.toString() shouldBe "com.futuremind.iossuspendwrapper.SuspendWrapper<kotlin.collections.List<kotlin.collections.Map<kotlin.Int, com.futuremind.kmm101.test.Whatever>>>"
        }
    }

    private fun testThrowsCompilationError(source: SourceFile, expectedMessage: String) =
        testThrowsCompilationError(arrayOf(source), expectedMessage)

    private fun testThrowsCompilationError(
        sources: Array<SourceFile>,
        expectedMessage: String
    ){
        val compilationResult = prepareCompilation(*sources).compile()
        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        compilationResult.messages shouldContain expectedMessage
    }

    private fun compileAndReturnGeneratedClass(
        source: SourceFile,
        generatedClassCanonicalName: String
    ): KClass<out Any> {
        val compilationResult = prepareCompilation(source).compile()
        val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
        return generatedClass.kotlin
    }

    private fun prepareCompilation(vararg sourceFiles: SourceFile) = KotlinCompilation()
        .apply {
            workingDir = tempDir
            annotationProcessors = listOf(Processor())
            inheritClassPath = true
            sources = sourceFiles.asList()
            verbose = false
        }

}