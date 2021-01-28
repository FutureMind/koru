package com.futuremind.iossuspendwrapper.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.reflect.KClass

fun testThrowsCompilationError(
    source: SourceFile,
    expectedMessage: String,
    tempDir: File
) = testThrowsCompilationError(listOf(source), expectedMessage, tempDir)

fun testThrowsCompilationError(
    sources: List<SourceFile>,
    expectedMessage: String,
    tempDir: File
) {
    val compilationResult = prepareCompilation(sources, tempDir).compile()
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    compilationResult.messages shouldContain expectedMessage
}

fun compileAndReturnGeneratedClass(
    source: SourceFile,
    generatedClassCanonicalName: String,
    tempDir: File
): KClass<out Any> {
    val compilationResult = prepareCompilation(source, tempDir).compile()
    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
    return generatedClass.kotlin
}

fun prepareCompilation(
    sourceFile: SourceFile,
    tempDir: File
) = prepareCompilation(listOf(sourceFile), tempDir)

fun prepareCompilation(
    sourceFiles: List<SourceFile>,
    tempDir: File
) = KotlinCompilation()
    .apply {
        workingDir = tempDir
        annotationProcessors = listOf(Processor())
        inheritClassPath = true
        sources = sourceFiles
        verbose = false
    }