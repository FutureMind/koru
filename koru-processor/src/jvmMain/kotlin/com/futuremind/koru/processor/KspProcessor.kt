package com.futuremind.koru.processor

import com.futuremind.koru.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic.Kind.ERROR


class KoruProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KspProcessor(
            options = environment.options,
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class KspProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val supportedAnnotations = setOf(
        ToNativeClass::class.qualifiedName,
        ToNativeInterface::class.qualifiedName,
        ExportedScopeProvider::class.qualifiedName
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val scopeProvidersSymbols = resolver
            .getSymbolsWithAnnotation(ExportedScopeProvider::class.qualifiedName!!)

        scopeProvidersSymbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(ScopeProviderVisitor(), Unit) }

        val unableToProcess = scopeProvidersSymbols.filterNot { it.validate() }

        return unableToProcess.toList()

    }

    inner class ScopeProviderVisitor() : KSDefaultVisitor<Unit, Unit>() {

        override fun defaultHandler(node: KSNode, data: Unit) {
            TODO("Not yet implemented brrrr")
        }

    }

}
