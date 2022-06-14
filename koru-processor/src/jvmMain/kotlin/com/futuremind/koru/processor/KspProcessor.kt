package com.futuremind.koru.processor

import com.futuremind.koru.ExportedScopeProvider
import com.futuremind.koru.ScopeProvider
import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*
import java.util.*
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass


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

        val scopeProvidersSymbols = resolver.getClassDeclarationsOf(ExportedScopeProvider::class)
        val interfaceSymbols = resolver.getClassDeclarationsOf(ToNativeInterface::class)
        val classSymbols = resolver.getClassDeclarationsOf(ToNativeClass::class)

        val generatedInterfaces = mutableMapOf<TypeName, GeneratedInterfaceSpec>()

        scopeProvidersSymbols
            .forEach { it.accept(ScopeProviderVisitor(codeGenerator), Unit) }

        interfaceSymbols.sortByInheritance().forEach { classDeclaration ->

            val originalTypeSpec = classDeclaration.toTypeSpec()
            val annotation = classDeclaration.getAnnotationsByType(ToNativeInterface::class).first()
            val originalTypeName = classDeclaration.toClassName()
            val newTypeName = interfaceName(annotation, originalTypeName.simpleName)

            val generatedType = WrapperInterfaceBuilder(
                originalTypeName = originalTypeName,
                originalTypeSpec = originalTypeSpec,
                newTypeName = newTypeName,
                generatedInterfaces = generatedInterfaces
            ).build()

            generatedInterfaces[originalTypeName] = GeneratedInterfaceSpec(
                originalTypeName,
                ClassName(originalTypeName.packageName, newTypeName),
                generatedType
            )

            codeGenerator.writeFile(
                packageName = originalTypeName.packageName,
                fileName = newTypeName
            ) { addType(generatedType) }

        }

        classSymbols.forEach { classDeclaration ->

            val originalTypeSpec = classDeclaration.toTypeSpec()
            val annotation = classDeclaration.getAnnotationsByType(ToNativeClass::class).first()
            val originalTypeName = classDeclaration.toClassName()
            val newTypeName = className(annotation, originalTypeName.simpleName)

            val generatedType = WrapperClassBuilder(
                originalTypeName = originalTypeName,
                originalTypeSpec = originalTypeSpec,
                newTypeName = newTypeName,
                generatedInterfaces = generatedInterfaces,
                scopeProviderMemberName = null, //TODO obtainScopeProviderMemberName(annotation, scopeProviders),
                freezeWrapper = annotation.freeze
            ).build()

            codeGenerator.writeFile(
                packageName = originalTypeName.packageName,
                fileName = newTypeName
            ) { addType(generatedType) }

        }

        val unableToProcess = (scopeProvidersSymbols + interfaceSymbols + classSymbols)
            .filterNot { it.validate() }

        return unableToProcess.toList()

    }

    private fun Resolver.getClassDeclarationsOf(clazz: KClass<out Annotation>) =
        getSymbolsWithAnnotation(clazz.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

    private fun CodeGenerator.writeFile(
        packageName: String,
        fileName: String,
        builder: FileSpec.Builder.() -> FileSpec.Builder
    ) = FileSpec.builder(packageName, fileName)
        .builder()
        .build()
        .writeTo(this, Dependencies(aggregating = false))

    //todo extract
    private fun KSClassDeclaration.toTypeSpec(): TypeSpec {
        val builder = when (this.isAbstract()) {
            true -> TypeSpec.interfaceBuilder(this.toClassName())
            false -> TypeSpec.classBuilder(this.toClassName())
        }

        return builder
            .addModifiers(modifiers.map { it.toKModifier()!! })
            .addSuperinterfaces(
                superTypes.toList().map { it.toTypeName() }
            )
            .addFunctions(
                getDeclaredFunctions()
                    .filterNot { it.isConstructor() }
                    .toList()
                    .map { it.toFunSpec() }
            )
            .addProperties(
                getDeclaredProperties()
                    .toList()
                    .map { it.toPropertySpec() }
            )
            .build()
    }

    private fun KSFunctionDeclaration.toFunSpec(): FunSpec =
        FunSpec.builder(simpleName.asString())
            .addModifiers(modifiers.map { it.toKModifier()!! })
            .addParameters(parameters.map { it.toParameterSpec() })
            .returns(returnType!!.toTypeName())
            .build()

    private fun KSValueParameter.toParameterSpec() = ParameterSpec.builder(
        name = this.name!!.getShortName(),
        type = this.type.toTypeName()
    ).build()

    private fun KSPropertyDeclaration.toPropertySpec(): PropertySpec =
        PropertySpec.builder(simpleName.asString(), type.toTypeName())
            .addModifiers(modifiers.map { it.toKModifier()!! })
            .build()

    inner class ScopeProviderVisitor(private val codeGenerator: CodeGenerator) : KSVisitorVoid() {

        @OptIn(KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

            val scopeProviderClassName = classDeclaration.toClassName()
            val scopePropertyName = "exportedScopeProvider_" +
                    scopeProviderClassName.simpleName.replaceFirstChar { it.lowercase(Locale.ROOT) }

            classDeclaration.assertExtendsScopeProvider()

            val propertySpec = ScopeProviderBuilder(
                scopeProviderClassName,
                scopePropertyName
            ).build()

            codeGenerator.writeFile(
                packageName = scopeProviderClassName.packageName,
                fileName = "${scopeProviderClassName.simpleName}Container"
            ) { addProperty(propertySpec) }

        }

        private fun KSClassDeclaration.assertExtendsScopeProvider() {
            if (superTypes.toList()
                    .map { it.toTypeName() }
                    .contains(ScopeProvider::class.asTypeName())
                    .not()
            ) {
                wrongScopeProviderSupertype()
            }
        }
    }
}
