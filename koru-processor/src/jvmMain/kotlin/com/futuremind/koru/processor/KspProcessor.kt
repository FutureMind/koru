package com.futuremind.koru.processor

import com.futuremind.koru.ExportedScopeProvider
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.*
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion


class KoruProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = KspProcessor(
        options = environment.options,
        codeGenerator = environment.codeGenerator,
        logger = environment.logger
    )
}

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class KspProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val scopeProvidersSymbols = resolver
            .getSymbolsWithAnnotation(ExportedScopeProvider::class.qualifiedName!!)

        scopeProvidersSymbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(ScopeProviderVisitor(codeGenerator), Unit) }

        val unableToProcess = scopeProvidersSymbols.filterNot { it.validate() }

        return unableToProcess.toList()

    }

    inner class ScopeProviderVisitor(private val codeGenerator: CodeGenerator) : KSVisitorVoid() {

        @OptIn(KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val scopeProviderClassName = classDeclaration.toClassName()
            val scopePropertyName = "exportedScopeProvider_" +
                    scopeProviderClassName.simpleName.replaceFirstChar { it.lowercase(Locale.ROOT) }

            val propertySpec = ScopeProviderBuilder(scopeProviderClassName, scopePropertyName).build()

            FileSpec
                .builder(scopeProviderClassName.packageName, "${scopeProviderClassName.simpleName}Container")
                .addProperty(propertySpec)
                .build()
                .writeTo(codeGenerator, Dependencies(aggregating = false))
        }

    }

}
