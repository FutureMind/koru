package com.futuremind.koru.processor

import com.futuremind.koru.ExportedScopeProvider
import com.futuremind.koru.ToNativeInterface
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*
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

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class KspProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val scopeProvidersSymbols = resolver
            .getSymbolsWithAnnotation(ExportedScopeProvider::class.qualifiedName!!)

        val interfaceSymbols = resolver
            .getSymbolsWithAnnotation(ToNativeInterface::class.qualifiedName!!)

        scopeProvidersSymbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(ScopeProviderVisitor(codeGenerator), Unit) }

        val generatedInterfaces = mutableMapOf<TypeName, GeneratedInterface>()

        interfaceSymbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()
            .sortByInheritance()
            .forEach { classDeclaration ->

                val builder = when(classDeclaration.isAbstract()){
                    true -> TypeSpec.interfaceBuilder(classDeclaration.toClassName())
                    false -> TypeSpec.classBuilder(classDeclaration.toClassName())
                }

                val originalTypeSpec = builder
                    .addModifiers(classDeclaration.modifiers.map { it.toKModifier()!! })
                    .addSuperinterfaces(classDeclaration.superTypes.toList().map { it.toTypeName() })
                    .addFunctions(
                        classDeclaration.getDeclaredFunctions()
                            .filterNot { it.isConstructor() }
                            .toList()
                            .map { it.toFunSpec() }
                    ).build()

                val annotation = classDeclaration.getAnnotationsByType(ToNativeInterface::class).first()
                val typeName = classDeclaration.toClassName()
                val newTypeName = annotation.name.nonEmptyOr("${typeName.simpleName}NativeProtocol")

                val generatedType = WrapperInterfaceBuilder(
                    originalTypeName = typeName,
                    originalTypeSpec = originalTypeSpec,
                    newTypeName = newTypeName,
                    generatedInterfaces = generatedInterfaces
                ).build()

                generatedInterfaces[typeName] = GeneratedInterface(
                    ClassName(typeName.packageName, newTypeName),
                    generatedType
                )

                //add generated

                println("KSP: \n${originalTypeSpec}")
                println("KSP gen: \n${generatedType}")

                FileSpec.builder(typeName.packageName, newTypeName)
                    .addType(generatedType)
                    .build()
                    .writeTo(codeGenerator, Dependencies(aggregating = false))

            }

        val unableToProcess = (scopeProvidersSymbols + interfaceSymbols)
            .filterNot { it.validate() }

        return unableToProcess.toList()

    }

    private fun KSFunctionDeclaration.toFunSpec() : FunSpec =
        FunSpec.builder(this.simpleName.asString())
            .addModifiers(this.modifiers.map { it.toKModifier()!! })
            .addParameters(this.parameters.map { it.toParameterSpec() })
            .returns(this.returnType!!.toTypeName())
            .build()

    private fun KSValueParameter.toParameterSpec() = ParameterSpec.builder(
            name = this.name!!.getShortName(),
            type = this.type.toTypeName()
        ).build()

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
