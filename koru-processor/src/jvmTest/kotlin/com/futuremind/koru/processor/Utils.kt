package com.futuremind.koru.processor

import com.tschuchort.compiletesting.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.reflect.KClass

fun testThrowsCompilationError(
    source: SourceFile,
    expectedMessage: String,
    tempDir: File,
    processorType: ProcessorType = ProcessorType.KAPT
) = testThrowsCompilationError(listOf(source), expectedMessage, tempDir, processorType)

fun testThrowsCompilationError(
    sources: List<SourceFile>,
    expectedMessage: String,
    tempDir: File,
    processorType: ProcessorType
) {
    val compilationResult = prepareCompilation(sources, tempDir, processorType).compile()
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    compilationResult.messages shouldContain expectedMessage
}

fun compileAndReturnGeneratedClass(
    source: SourceFile,
    generatedClassCanonicalName: String,
    tempDir: File,
    processorType: ProcessorType = ProcessorType.KAPT
): KClass<out Any> {
    val compilationResult = when (processorType) {
        ProcessorType.KAPT -> prepareCompilation(source, tempDir, processorType).compile()
        ProcessorType.KSP -> KSPRuntimeCompiler.compile(
            tempDir, prepareCompilation(source, tempDir, processorType)
        )
    }
//    debugPrintGenerated(compilationResult)
    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
    return generatedClass.kotlin
}


/**
 * Double compilation hack taken from
 * https://github.com/tschuchortdev/kotlin-compile-testing/issues/72
 */
object KSPRuntimeCompiler {

    fun compile(tempDir: File, compilation: KotlinCompilation): KotlinCompilation.Result {
        val pass1 = compilation.compile()
        require(pass1.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 1st pass \n ${pass1.messages}"
        }
        val pass2 = KotlinCompilation().apply {
            sources = compilation.kspGeneratedSourceFiles(tempDir) + compilation.sources
            verbose = false
            inheritClassPath = true
        }.compile()
        require(pass2.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 2nd pass \n ${pass2.messages}"
        }
        return pass2
    }

    private fun KotlinCompilation.kspGeneratedSourceFiles(tempDir: File): List<SourceFile> =
        kspGeneratedSources(tempDir)
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
            .toList()
}

fun debugPrintGenerated(compilationResult: KotlinCompilation.Result) {
    compilationResult.generatedFiles.forEach {
        println("\n\n" + it.absolutePath + "\n")
        println(it.readText().trim())
    }
}


fun prepareCompilation(
    sourceFile: SourceFile,
    tempDir: File,
    processorType: ProcessorType = ProcessorType.KAPT //todo only temp default
) = prepareCompilation(listOf(sourceFile), tempDir, processorType)

fun prepareCompilation(
    sourceFiles: List<SourceFile>,
    tempDir: File,
    processorType: ProcessorType = ProcessorType.KAPT
) = KotlinCompilation()
    .apply {
        workingDir = tempDir
        inheritClassPath = true
        sources = sourceFiles
        verbose = false
        when (processorType) {
            ProcessorType.KAPT -> {
                annotationProcessors = listOf(KaptProcessor())
            }
            ProcessorType.KSP -> {
                symbolProcessorProviders = listOf(KoruProcessorProvider())
                kspIncremental = false
            }
        }
    }


fun KClass<*>.member(methodName: String) =
    members.find { it.name == methodName }!!

fun KClass<*>.memberReturnType(methodName: String) = member(methodName).returnType.toString()

fun Collection<File>.getContentByFilename(filename: String) = this
    .find { it.name == filename }!!
    .readText()
    .trim()

const val defaultClassNameSuffix = "Native"
const val defaultInterfaceNameSuffix = "NativeProtocol"

fun KotlinCompilation.Result.generatedFiles(
    processorType: ProcessorType,
    tempDir: File
) = when (processorType) {
    ProcessorType.KAPT -> this.generatedFiles
    ProcessorType.KSP -> kspGeneratedSources(tempDir)
}

private fun kspGeneratedSources(tempDir: File): List<File> {
    val kspWorkingDir = tempDir.resolve("ksp")
    val kspGeneratedDir = kspWorkingDir.resolve("sources")
    val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
    val javaGeneratedDir = kspGeneratedDir.resolve("java")
    return kotlinGeneratedDir.walkTopDown().toList() +
            javaGeneratedDir.walkTopDown()
}
