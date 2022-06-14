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
    val compilationResult = when(processorType) {
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

////TODO dry
//fun compileAndReturnKspGeneratedClass(
//    source: SourceFile,
//    generatedClassCanonicalName: String,
//    tempDir: File
//): KClass<out Any> {
//    val compilationResult = KSPRuntimeCompiler.compile(
//        tempDir, prepareKspCompilation(listOf(source), tempDir)
//    )
////    val compilationResultKapt = prepareCompilation(listOf(source), tempDir).compile()
////    debugPrintGenerated(compilationResult)
//    val generatedClass = compilationResult.classLoader.loadClass(generatedClassCanonicalName)
//    compilationResult.exitCode shouldBe KotlinCompilation.ExitCode.OK
//    return generatedClass.kotlin
//}

object KSPRuntimeCompiler {

    fun compile(tempDir: File, compilation: KotlinCompilation): KotlinCompilation.Result {
        val pass1 = compilation.compile()
        require(pass1.exitCode == KotlinCompilation.ExitCode.OK) {
            "Cannot do the 1st pass"
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

//    private fun KotlinCompilation.kspGeneratedSourceFiles(tempDir: File): List<SourceFile>
//        = kspSourcesDir.resolve("kotlin")
//            .walk()
//            .filter { it.isFile }
//            .map { SourceFile.fromPath(it.absoluteFile) }
//            .toList()

    private fun KotlinCompilation.kspGeneratedSourceFiles(tempDir: File): List<SourceFile> =
        kspGeneratedSources(tempDir)
//            .apply { println("aaa"+this.joinToString { "\n${it.isFile}" }) }
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
//            .apply { println("bbb"+this) }
            .toList()
}

fun debugPrintGenerated(compilationResult: KotlinCompilation.Result) {
    compilationResult.generatedFiles.forEach {
        println("\n\n"+it.absolutePath+"\n")
        println(it.readText().trim())
    }
}


fun prepareCompilation(
    sourceFile: SourceFile,
    tempDir: File,
    processorType: ProcessorType = ProcessorType.KAPT //todo only temp default
) = prepareCompilation(listOf(sourceFile), tempDir)

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
        when(processorType){
            ProcessorType.KAPT -> {
                annotationProcessors =  listOf(KaptProcessor())
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

fun kspGeneratedSources(tempDir: File): List<File> {
    val kspWorkingDir = tempDir.resolve("ksp")
    val kspGeneratedDir = kspWorkingDir.resolve("sources")
    val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
    val javaGeneratedDir = kspGeneratedDir.resolve("java")
    return kotlinGeneratedDir.walkTopDown().toList() +
            javaGeneratedDir.walkTopDown()
}

//logging: Created temporary working directory at /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638
//logging: No services were given. Not running kapt steps.
//v: Using Kotlin home directory <no_path>
//v: Using JDK home directory /Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home
//v: Scripting plugin will not be loaded: not all required jars are present in the classpath (missing files: [./kotlin-scripting-compiler.jar, ./kotlin-scripting-compiler-impl.jar, ./kotlinx-coroutines-core-jvm.jar, ./kotlin-scripting-common.jar, ./kotlin-scripting-jvm.jar, ./kotlin-scripting-js.jar, ./js.engines.jar])
//v: Using JVM IR backend
//v: Configuring the compilation environment
//v: Loading modules: [java.se, jdk.accessibility, jdk.attach, jdk.compiler, jdk.dynalink, jdk.httpserver, jdk.incubator.foreign, jdk.jartool, jdk.javadoc, jdk.jconsole, jdk.jdi, jdk.jfr, jdk.jshell, jdk.jsobject, jdk.management, jdk.management.jfr, jdk.net, jdk.nio.mapmode, jdk.sctp, jdk.security.auth, jdk.security.jgss, jdk.unsupported, jdk.unsupported.desktop, jdk.xml.dom, java.base, java.compiler, java.datatransfer, java.desktop, java.xml, java.instrument, java.logging, java.management, java.management.rmi, java.rmi, java.naming, java.net.http, java.prefs, java.scripting, java.security.jgss, java.security.sasl, java.sql, java.transaction.xa, java.sql.rowset, java.xml.crypto, jdk.internal.jvmstat, jdk.management.agent, jdk.jdwp.agent, jdk.internal.ed, jdk.internal.le, jdk.internal.opt]
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (4, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (5, 8): Unresolved reference: kotlinx
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (7, 2): Unresolved reference: ToNativeInterface
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (12, 2): Unresolved reference: ToNativeInterface
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (17, 2): Unresolved reference: ToNativeInterface
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (23, 2): Unresolved reference: ToNativeInterface
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (32, 36): Unresolved reference: Flow
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (35, 2): Unresolved reference: ToNativeInterface
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/Kotlin-Compilation10739665445976763638/sources/multipleSuperInterfaces3.kt: (42, 45): Unresolved reference: Flow
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/ANativeProtocol.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/ANativeProtocol.kt: (8, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/BNativeProtocol.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/BNativeProtocol.kt: (8, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/CNativeProtocol.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/CNativeProtocol.kt: (8, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/CNativeProtocol.kt: (10, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/DNativeProtocol.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/DNativeProtocol.kt: (8, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/DNativeProtocol.kt: (10, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/DNativeProtocol.kt: (12, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/DNativeProtocol.kt: (14, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (3, 23): Unresolved reference: koru
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (6, 8): Unresolved reference: kotlinx
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (9, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (11, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (13, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (15, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (17, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (19, 32): Unresolved reference: SuspendWrapper
//e: /var/folders/8n/1534k2kd20g0npbvwqbpq_pm0000gn/T/junit9500077738613404005/ksp/sources/kotlin/com/futuremind/kmm101/test/MultipleInterfacesExampleNativeProtocol.kt: (19, 47): Unresolved reference: Flow
