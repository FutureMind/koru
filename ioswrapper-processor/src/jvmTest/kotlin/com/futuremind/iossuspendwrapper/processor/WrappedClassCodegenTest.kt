package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File


//TODO this is just a stub, more tests required
class WrappedClassCodegenTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should throw if @ExportedScopeProvider is applied to class which doesn't extend ScopeProvider`() {

        val source = SourceFile.kotlin(
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
        )

        val compilationResult = prepareCompilation(source).compile()

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        compilationResult.messages shouldContain "ExportedScopeProvider can only be applied to a class extending ScopeProvider interface"


    }

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

    private fun prepareCompilation(vararg sourceFiles: SourceFile) = KotlinCompilation()
        .apply {
            workingDir = tempDir
            annotationProcessors = listOf(Processor())
            inheritClassPath = true
            sources = sourceFiles.asList()
            verbose = false
        }

}