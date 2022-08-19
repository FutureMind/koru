package com.futuremind.koru.processor.utils

import com.futuremind.koru.processor.KaptProcessor
import com.futuremind.koru.processor.KoruProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.reflect.KClass

fun testThrowsCompilationError(
    source: SourceFile,
    expectedMessage: String,
    tempDir: File,
    processorType: ProcessorType
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

fun compile(
    sources: List<SourceFile>,
    tempDir: File,
    processorType: ProcessorType
) = when (processorType) {
    ProcessorType.KAPT -> prepareCompilation(sources, tempDir, processorType).compile()
    ProcessorType.KSP -> KSPRuntimeCompiler.compile(
        tempDir, prepareCompilation(sources, tempDir, processorType)
    )
}.apply {
    exitCode shouldBe KotlinCompilation.ExitCode.OK
}

fun compileAndReturnGeneratedClass(
    source: SourceFile,
    tempDir: File,
    processorType: ProcessorType,
    generatedClassCanonicalName: String
): KClass<out Any> {
    val compilationResult = compile(listOf(source), tempDir, processorType)
    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
    return generatedClass.kotlin
}

private fun prepareCompilation(
    sourceFiles: List<SourceFile>,
    tempDir: File,
    processorType: ProcessorType
) = KotlinCompilation().apply {
    workingDir = tempDir
    inheritClassPath = true
    sources = sourceFiles
    kotlincArguments = listOf("-Xmulti-platform") //TODO https://github.com/tschuchortdev/kotlin-compile-testing/issues/303
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

fun KotlinCompilation.Result.generatedFiles(
    processorType: ProcessorType,
    tempDir: File
) = when (processorType) {
    ProcessorType.KAPT -> this.generatedFiles
    ProcessorType.KSP -> kspGeneratedSources(tempDir)
}

fun Collection<File>.getContentByFilename(filename: String) = this
    .find { it.name == filename }!!
    .readText()
    .trim()

/**
 * Double compilation hack taken from
 * https://github.com/tschuchortdev/kotlin-compile-testing/issues/72
 */
private object KSPRuntimeCompiler {

    fun compile(tempDir: File, compilation: KotlinCompilation): KotlinCompilation.Result {
        val pass1 = compilation.compile()
        require(pass1.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 1st pass \n ${pass1.messages}"
        }
        debugPrintGenerated(kspGeneratedSources(tempDir))
        val pass2 = KotlinCompilation().apply {
            sources = compilation.kspGeneratedSourceFiles(tempDir) + compilation.sources
            inheritClassPath = true
            kotlincArguments = listOf("-Xmulti-platform") //TODO https://github.com/tschuchortdev/kotlin-compile-testing/issues/303
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

private fun kspGeneratedSources(tempDir: File): List<File> {
    val kspWorkingDir = tempDir.resolve("ksp")
    val kspGeneratedDir = kspWorkingDir.resolve("sources")
    val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
    val javaGeneratedDir = kspGeneratedDir.resolve("java")
    return kotlinGeneratedDir.walkTopDown().toList() +
            javaGeneratedDir.walkTopDown()
}

fun debugPrintGenerated(files: Collection<File>) = files
    .filter { it.isFile }
    .forEach {
    println("\n\n"+it.absolutePath+"\n")
    println(it.readText().trim())
}
