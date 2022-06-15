package com.futuremind.koru.processor

import com.futuremind.koru.ExportedScopeProvider
import com.futuremind.koru.ScopeProvider
import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.futuremind.koru.processor.builders.ScopeProviderBuilder
import com.futuremind.koru.processor.builders.WrapperClassBuilder
import com.futuremind.koru.processor.builders.WrapperInterfaceBuilder
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import java.io.File
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR


const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@OptIn(KotlinPoetMetadataPreview::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class KaptProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        ToNativeClass::class.java.canonicalName,
        ToNativeInterface::class.java.canonicalName,
        ExportedScopeProvider::class.java.canonicalName
    )

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ) = try {

        val kaptGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: throw IllegalStateException("Cannot access kaptKotlinGeneratedDir")

        val classInspector = ElementsClassInspector.create(
            processingEnv.elementUtils,
            processingEnv.typeUtils
        )

        val scopeProviders = mutableMapOf<ClassName, PropertySpec>()
        val generatedInterfaces = mutableMapOf<TypeName, GeneratedInterfaceSpec>()

        roundEnv
            .getElementsAnnotatedWith(ExportedScopeProvider::class.java)
            .asSequence()
            .map { element ->
                generateScopeProvider(
                    element = element,
                    classInspector = classInspector
                )
            }
            .forEach {
                scopeProviders[it.newTypeName] = it.newSpec
                kaptGeneratedDir.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = it.newTypeName.simpleName + "Container"
                ) { addProperty(it.newSpec) }
            }

        roundEnv.getElementsAnnotatedWith(ToNativeInterface::class.java)
            .sortByInheritance(classInspector, processingEnv)
            .asSequence()
            .map { element ->
                generateInterface(
                    element = element,
                    classInspector = classInspector,
                    generatedInterfaces = generatedInterfaces,
                )
            }
            .forEach {
                generatedInterfaces[it.originalTypeName] = it
                kaptGeneratedDir.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = it.newTypeName.simpleName
                ) { addType(it.newSpec) }
            }

        roundEnv.getElementsAnnotatedWith(ToNativeClass::class.java)
            .asSequence()
            .map { element ->
                generateClass(
                    element = element,
                    classInspector = classInspector,
                    generatedInterfaces = generatedInterfaces,
                    scopeProviders = scopeProviders
                )
            }
            .forEach {
                kaptGeneratedDir.writeFile(
                    packageName = it.newTypeName.packageName,
                    fileName = it.newTypeName.simpleName
                ) { addType(it.newSpec) }
            }

        true

    } catch (e: Throwable) {
        e.printStackTrace()
        processingEnv.messager.printMessage(ERROR, "${e::class.simpleName}: ${e.message}")
        false
    }

    private fun generateScopeProvider(
        element: Element,
        classInspector: ClassInspector
    ): GeneratedPropertySpec {

        val packageName = element.getPackage(processingEnv)
        val scopeClassSpec = (element as TypeElement).toTypeSpec(classInspector)
        val originalClassName = element.getClassName(processingEnv)
        val scopeProviderClassName = ClassName(packageName, scopeClassSpec.name.toString())
        val scopePropertyName =
            "exportedScopeProvider_" + scopeClassSpec.name!!.replaceFirstChar { it.lowercase(Locale.ROOT) }

        scopeClassSpec.assertExtendsScopeProvider()

        val newPropertySpec = ScopeProviderBuilder(
            scopeProviderClassName,
            scopePropertyName
        ).build()

        return GeneratedPropertySpec(
            originalTypeName = originalClassName,
            newTypeName = originalClassName,
            newSpec = newPropertySpec
        )

    }

    private fun generateInterface(
        element: Element,
        classInspector: ClassInspector,
        generatedInterfaces: Map<TypeName, GeneratedInterfaceSpec>
    ): GeneratedInterfaceSpec {

        val originalTypeName = element.getClassName(processingEnv)
        val originalTypeSpec = (element as TypeElement).toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeInterface::class.java)
        val newTypeName = interfaceName(annotation, originalTypeName.simpleName)

        val newTypeSpec = WrapperInterfaceBuilder(
            originalTypeName = originalTypeName,
            originalTypeSpec = originalTypeSpec,
            newTypeName = newTypeName,
            generatedInterfaces = generatedInterfaces
        ).build()

        val newInterfaceName = ClassName(originalTypeName.packageName, newTypeName)

        return GeneratedInterfaceSpec(
            originalTypeName = originalTypeName,
            newTypeName = newInterfaceName,
            newSpec = newTypeSpec
        )

    }

    private fun generateClass(
        element: Element,
        classInspector: ClassInspector,
        generatedInterfaces: Map<TypeName, GeneratedInterfaceSpec>,
        scopeProviders: Map<ClassName, PropertySpec>
    ): GeneratedClassSpec {

        val originalTypeName = element.getClassName(processingEnv)
        val originalTypeSpec = (element as TypeElement).toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeClass::class.java)
        val newTypeName = className(annotation, originalTypeName.simpleName)

        val newTypeSpec = WrapperClassBuilder(
            originalTypeName = originalTypeName,
            originalTypeSpec = originalTypeSpec,
            newTypeName = newTypeName,
            generatedInterfaces = generatedInterfaces,
            scopeProviderMemberName = findMatchingScopeProvider(
                annotation.launchOnScopeTypeName(),
                scopeProviders
            ),
            freezeWrapper = annotation.freeze
        ).build()

        return GeneratedClassSpec(
            originalTypeName = originalTypeName,
            newTypeName = ClassName(originalTypeName.packageName, newTypeName),
            newSpec = newTypeSpec
        )

    }

    private fun String.writeFile(
        packageName: String,
        fileName: String,
        builder: FileSpec.Builder.() -> FileSpec.Builder
    ) = FileSpec.builder(packageName, fileName)
        .builder()
        .build()
        .writeTo(File(this))

    private fun TypeSpec.assertExtendsScopeProvider() {
        if (!superinterfaces.contains(ScopeProvider::class.asTypeName())) {
            wrongScopeProviderSupertype()
        }
    }

}

//TODO refactor

internal fun Element.getPackage(processingEnv: ProcessingEnvironment) =
    processingEnv.elementUtils.getPackageOf(this).toString()

internal fun Element.getClassName(processingEnv: ProcessingEnvironment) =
    ClassName(this.getPackage(processingEnv), this.simpleName.toString())
