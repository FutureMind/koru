package com.futuremind.iossuspendwrapper.processor

import com.futuremind.iossuspendwrapper.ExportedScopeProvider
import com.futuremind.iossuspendwrapper.ScopeProvider
import com.futuremind.iossuspendwrapper.WrapForIos
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
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
import javax.tools.Diagnostic.Kind.*


const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        WrapForIos::class.java.canonicalName,
        ExportedScopeProvider::class.java.canonicalName
    )

    @OptIn(KotlinPoetMetadataPreview::class)
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

        val scopeProviders: Map<ClassName, PropertySpec> = roundEnv
            .getElementsAnnotatedWith(ExportedScopeProvider::class.java)
            .map { element ->
                generateScopeProvider(
                    element = element,
                    classInspector = classInspector,
                    targetDir = kaptGeneratedDir
                )
            }
            .toMap()

        val annotatedElements = roundEnv.getElementsAnnotatedWith(WrapForIos::class.java)

        val generatedInterfacesFromAnnotatedInterfaces = annotatedElements
            .filter { it.kind.isInterface }
            .map { element ->
                generateInterfaceFromInterface(
                    element = element,
                    classInspector = classInspector,
                    targetDir = kaptGeneratedDir
                )
            }
            .toMap()

        annotatedElements
            .filter { it.kind.isClass }
            .forEach { element ->
                generateWrappedClasses(
                    element = element,
                    classInspector = classInspector,
                    generatedInterfacesNames = generatedInterfacesFromAnnotatedInterfaces,
                    scopeProviders = scopeProviders,
                    kaptGeneratedDir = kaptGeneratedDir
                )
            }

        true

    } catch (e: Throwable) {
        processingEnv.messager.printMessage(ERROR, "${e::class.simpleName}: ${e.message}")
        false
    }

    @KotlinPoetMetadataPreview
    private fun generateScopeProvider(
        element: Element,
        classInspector: ClassInspector,
        targetDir: String
    ): Pair<ClassName, PropertySpec> {

        val packageName = element.getPackage(processingEnv)
        val scopeClassSpec = (element as TypeElement)
            .toImmutableKmClass()
            .toTypeSpec(classInspector)

        scopeClassSpec.assertExtendsScopeProvider()

        val originalClassName = element.getClassName(processingEnv)
        val propertySpec = ScopeProviderBuilder(packageName, scopeClassSpec).build()

        FileSpec
            .builder(originalClassName.packageName, "${originalClassName.simpleName}Container")
            .addProperty(propertySpec)
            .build()
            .writeTo(File(targetDir))

        return originalClassName to propertySpec

    }

    @KotlinPoetMetadataPreview
    private fun generateInterfaceFromInterface(
        element: Element,
        classInspector: ClassInspector,
        targetDir: String
    ): Pair<OriginalInterfaceName, GeneratedInterfaceName> {

        val interfaceName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toImmutableKmClass().toTypeSpec(classInspector)
        val annotation = element.getAnnotation(WrapForIos::class.java)
        val newTypeName = annotation.className.nonEmptyOr("${interfaceName.simpleName}Ios")

        val generatedType = WrapperInterfaceBuilder(newTypeName, typeSpec).build()

        FileSpec.builder(interfaceName.packageName, newTypeName)
            .addType(generatedType)
            .build()
            .writeTo(File(targetDir))

        val newInterfaceName = ClassName(interfaceName.packageName, newTypeName)

        return interfaceName to newInterfaceName

    }

    @KotlinPoetMetadataPreview
    private fun generateWrappedClasses(
        element: Element,
        classInspector: ClassInspector,
        generatedInterfacesNames: Map<OriginalInterfaceName, GeneratedInterfaceName>,
        scopeProviders: Map<ClassName, PropertySpec>,
        kaptGeneratedDir: String
    ) {

        val originalClassName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toImmutableKmClass().toTypeSpec(classInspector)
        val annotation = element.getAnnotation(WrapForIos::class.java)

        val generatedClassName =
            annotation.className.nonEmptyOr("${originalClassName.simpleName}Ios")

        val interfaceFromClass: GeneratedSuperInterface? = generateInterfaceFromClass(
            shouldGenerate = annotation.generateInterface,
            generatedInterfaceName = annotation
                .generatedInterfaceName
                .nonEmptyOr("${generatedClassName}Protocol"),
            originalClassTypeSpec = typeSpec,
            originalClassName = originalClassName,
            kaptGeneratedDir = kaptGeneratedDir
        )

        val originalToGeneratedInterfaceName: OriginalToGeneratedInterfaceName? = typeSpec
            .superinterfaces.keys
            .matchGeneratedInterfaceName(generatedInterfacesNames)

        val classToGenerateSpec = WrapperClassBuilder(
            wrappedClassName = originalClassName,
            poetMetadataSpec = typeSpec,
            newTypeName = generatedClassName,
            interfaceGeneratedFromClass = interfaceFromClass,
            wrapperGeneratedInterface = originalToGeneratedInterfaceName,
            scopeProviderSpec = obtainScopeProviderSpec(annotation, scopeProviders)
        ).build()

        FileSpec.builder(originalClassName.packageName, generatedClassName)
            .addType(classToGenerateSpec)
            .build()
            .writeTo(File(kaptGeneratedDir))

    }

    private fun generateInterfaceFromClass(
        shouldGenerate: Boolean,
        generatedInterfaceName: String,
        originalClassTypeSpec: TypeSpec,
        originalClassName: ClassName,
        kaptGeneratedDir: String
    ): GeneratedSuperInterface? {

        if (!shouldGenerate) return null

        val generatedType = WrapperInterfaceBuilder(
            newTypeName = generatedInterfaceName,
            poetMetadataSpec = originalClassTypeSpec
        ).build()

        FileSpec.builder(originalClassName.packageName, generatedInterfaceName)
            .addType(generatedType)
            .build()
            .writeTo(File(kaptGeneratedDir))

        return GeneratedSuperInterface(
            ClassName(originalClassName.packageName, generatedInterfaceName),
            generatedType
        )
    }

    private fun Set<TypeName>.matchGeneratedInterfaceName(
        allGeneratedInterfaces: Map<OriginalInterfaceName, GeneratedInterfaceName>
    ): OriginalToGeneratedInterfaceName? = this
        .find { allGeneratedInterfaces[it] != null }
        ?.let { originalName ->
            OriginalToGeneratedInterfaceName(
                originalName = originalName,
                generatedName = allGeneratedInterfaces[originalName]!!
            )
        }

    private fun obtainScopeProviderSpec(
        annotation: WrapForIos,
        scopeProviders: Map<ClassName, PropertySpec>
    ): PropertySpec? {
        //this is the dirtiest hack ever but it works :O
        //there probably is some way of doing this via kotlinpoet-metadata
        //https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        var typeMirror: TypeMirror? = null
        try {
            annotation.launchOnScope
        } catch (e: MirroredTypeException) {
            typeMirror = e.typeMirror
        }
        return scopeProviders[typeMirror?.asTypeName()]
    }

    private fun String.nonEmptyOr(or: String) = when (this.isEmpty()) {
        true -> or
        false -> this
    }

    private fun Element.getPackage(processingEnv: ProcessingEnvironment) =
        processingEnv.elementUtils.getPackageOf(this).toString()

    private fun Element.getClassName(processingEnv: ProcessingEnvironment) =
        ClassName(this.getPackage(processingEnv), this.simpleName.toString())

    private fun TypeSpec.assertExtendsScopeProvider() {
        if (!superinterfaces.contains(ScopeProvider::class.asTypeName())) {
            throw IllegalArgumentException("ExportedScopeProvider can only be applied to a class extending ScopeProvider interface")
        }
    }

}

data class GeneratedSuperInterface(val name: TypeName, val typeSpec: TypeSpec)

data class OriginalToGeneratedInterfaceName(val originalName: TypeName, val generatedName: TypeName)
typealias OriginalInterfaceName = ClassName
typealias GeneratedInterfaceName = ClassName