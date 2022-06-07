package com.futuremind.koru.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
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
//    debugPrintGenerated(compilationResult)
    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
    return generatedClass.kotlin
}

//TODO dry
fun compileAndReturnKspGeneratedClass(
    source: SourceFile,
    generatedClassCanonicalName: String,
    tempDir: File
): KClass<out Any> {
    val compilationResult = prepareKspCompilation(listOf(source), tempDir).compile()
//    debugPrintGenerated(compilationResult)
    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
    return generatedClass.kotlin
}

fun debugPrintGenerated(compilationResult: KotlinCompilation.Result) {
    compilationResult.generatedFiles.forEach {
        println("\n\n"+it.absolutePath+"\n")
        println(it.readText().trim())
    }
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
        annotationProcessors = listOf(KaptProcessor())
        inheritClassPath = true
        sources = sourceFiles
        verbose = false
    }

//TODO dry
fun prepareKspCompilation(
    sourceFiles: List<SourceFile>,
    tempDir: File
) = KotlinCompilation()
    .apply {
        workingDir = tempDir
        symbolProcessorProviders = listOf(KoruProcessorProvider())
        inheritClassPath = true
        sources = sourceFiles
        verbose = false
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