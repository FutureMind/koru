package com.futuremind.koru.processor

import com.futuremind.koru.ExportedScopeProvider
import com.futuremind.koru.ScopeProvider
import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.futuremind.koru.processor.builders.ScopeProviderBuilder
import com.futuremind.koru.processor.builders.WrapperClassBuilder
import com.futuremind.koru.processor.builders.WrapperInterfaceBuilder
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass


class KoruProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        KspProcessor(environment.codeGenerator)
}

@OptIn(KspExperimental::class)
class KspProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val scopeProvidersSymbols = resolver.getClassDeclarationsOf(ExportedScopeProvider::class)
        val interfaceSymbols = resolver.getClassDeclarationsOf(ToNativeInterface::class)
        val classSymbols = resolver.getClassDeclarationsOf(ToNativeClass::class)

        val scopeProviders = mutableMapOf<ClassName, PropertySpec>()
        val generatedInterfaces = mutableMapOf<TypeName, GeneratedInterfaceSpec>()

        scopeProvidersSymbols
            .asSequence()
            .map { generateScopeProvider(it) }
            .forEach {
                scopeProviders[it.newTypeName] = it.newSpec
                codeGenerator.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = scopeProviderContainerName(it)
                ) { addProperty(it.newSpec) }
            }

        interfaceSymbols
            .sortByInheritance()
            .asSequence()
            .map { generateInterface(it, generatedInterfaces) }
            .forEach {
                generatedInterfaces[it.originalTypeName] = it
                codeGenerator.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = it.newTypeName.simpleName
                ) { addType(it.newSpec) }
            }

        classSymbols
            .asSequence()
            .map { generateClass(it, generatedInterfaces, scopeProviders) }
            .forEach {
                codeGenerator.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = it.newTypeName.simpleName
                ) { addType(it.newSpec) }

            }

        return (scopeProvidersSymbols + interfaceSymbols + classSymbols).filterNot { it.validate() }

    }

    private fun generateScopeProvider(classDeclaration: KSClassDeclaration): GeneratedPropertySpec {
        val scopeProviderClassName = classDeclaration.toClassName()
        val scopePropertyName = scopeProviderPropertyName(scopeProviderClassName)

        classDeclaration.assertExtendsScopeProvider()

        val propertySpec = ScopeProviderBuilder(
            scopeProviderClassName,
            scopePropertyName
        ).build()

        return GeneratedPropertySpec(
            originalTypeName = scopeProviderClassName,
            newTypeName = scopeProviderClassName,
            newSpec = propertySpec
        )
    }

    private fun generateInterface(
        classDeclaration: KSClassDeclaration,
        generatedInterfaces: MutableMap<TypeName, GeneratedInterfaceSpec>
    ): GeneratedInterfaceSpec {
        val originalTypeSpec = classDeclaration.toTypeSpec()
        val annotation =
            classDeclaration.getAnnotationsByType(ToNativeInterface::class).first()
        val originalTypeName = classDeclaration.toClassName()
        val newTypeName = interfaceName(annotation, originalTypeName.simpleName)

        val generatedType = WrapperInterfaceBuilder(
            originalTypeName = originalTypeName,
            originalTypeSpec = originalTypeSpec,
            newTypeName = newTypeName,
            generatedInterfaces = generatedInterfaces
        ).build()

        return GeneratedInterfaceSpec(
            originalTypeName,
            ClassName(originalTypeName.packageName, newTypeName),
            generatedType
        )
    }

    private fun generateClass(
        classDeclaration: KSClassDeclaration,
        generatedInterfaces: MutableMap<TypeName, GeneratedInterfaceSpec>,
        scopeProviders: MutableMap<ClassName, PropertySpec>
    ): GeneratedClassSpec {
        val originalTypeSpec = classDeclaration.toTypeSpec()
        val annotation = classDeclaration.getAnnotationsByType(ToNativeClass::class).first()
        val launchOnScopeTypeName = classDeclaration.getKsAnnotationByType(ToNativeClass::class)
            .arguments.first { it.name?.getShortName() == "launchOnScope" }
            .let { (it.value as? KSType)?.toTypeName() }
        val originalTypeName = classDeclaration.toClassName()
        val newTypeName = className(annotation, originalTypeName.simpleName)
        val scopeProviderMemberName = findMatchingScopeProvider(
            launchOnScopeTypeName,
            scopeProviders
        )

        val generatedType = WrapperClassBuilder(
            originalTypeName = originalTypeName,
            originalTypeSpec = originalTypeSpec,
            newTypeName = newTypeName,
            generatedInterfaces = generatedInterfaces,
            scopeProviderMemberName = scopeProviderMemberName,
            freezeWrapper = annotation.freeze
        ).build()

        return GeneratedClassSpec(
            originalTypeName = originalTypeName,
            newTypeName = ClassName(originalTypeName.packageName, newTypeName),
            newSpec = generatedType
        )
    }

    private fun <T : Annotation> KSAnnotated.getKsAnnotationByType(annotationKClass: KClass<T>): KSAnnotation {
        return this.annotations.filter {
            it.shortName.getShortName() == annotationKClass.simpleName && it.annotationType.resolve().declaration
                .qualifiedName?.asString() == annotationKClass.qualifiedName
        }.first()
    }

    private fun CodeGenerator.writeFile(
        packageName: String,
        fileName: String,
        builder: FileSpec.Builder.() -> FileSpec.Builder
    ) = FileSpec.builder(packageName, fileName)
        .builder()
        .build()
        .writeTo(this, Dependencies(aggregating = false))

    private fun KSClassDeclaration.assertExtendsScopeProvider() {
        if (superTypes.toList()
                .map { it.toTypeName() }
                .contains(ScopeProvider::class.asTypeName())
                .not()
        ) {
            wrongScopeProviderSupertype()
        }
    }

    private fun Resolver.getClassDeclarationsOf(clazz: KClass<out Annotation>) =
        getSymbolsWithAnnotation(clazz.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()

}
