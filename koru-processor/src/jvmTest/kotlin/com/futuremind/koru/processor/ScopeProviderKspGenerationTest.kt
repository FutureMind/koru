package com.futuremind.koru.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ScopeProviderKspGenerationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should generate top level property with scope provider`() {

        val source = SourceFile.kotlin(
            "scopeProvider2.kt",
            """
                package com.futuremind.kmm101.test
                
                import com.futuremind.koru.ExportedScopeProvider
                import com.futuremind.koru.ScopeProvider
                import kotlinx.coroutines.MainScope
                
                @ExportedScopeProvider
                class MainScopeProvider : ScopeProvider {
                    override val scope = MainScope()
                }
            """
        )

        val compilationResult = prepareKspCompilation(listOf(source), tempDir).compile()

        val generatedScopeProvider = kspGeneratedSources(tempDir)
            .getContentByFilename("MainScopeProviderContainer.kt")

        compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
        generatedScopeProvider shouldBe
                """
                    |package com.futuremind.kmm101.test
                    |
                    |public val exportedScopeProvider_mainScopeProvider: MainScopeProvider = MainScopeProvider()
                """.trimMargin().trim()

    }

    @Test
    fun `should generate complex inheritance hierarchy`() {

        val generatedType = compileAndReturnKspGeneratedClass(
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
                                suspend fun z(whatever: Int) : Flow<Float>
                            }
                            
                            @ToNativeInterface
                            class MultipleInterfacesExample : A, D, Z {
                                override suspend fun a(whatever: Int) : Float = TODO()
                                override suspend fun b(whatever: Int) : Float = TODO()
                                override suspend fun c(whatever: Int) : Float = TODO()
                                override suspend fun d(whatever: Int) : Float = TODO()
                                suspend fun e(whatever: Int) : Float = TODO()
                                override suspend fun z(whatever: Int) : Flow<Float> = TODO()
                            }
                        """
            ),
            generatedClassCanonicalName = "com.futuremind.kmm101.test.MultipleInterfacesExample$defaultInterfaceNameSuffix",
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


}
