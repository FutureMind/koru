package com.futuremind.iossuspendwrapper.processor

import com.futuremind.iossuspendwrapper.*
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
import javax.tools.Diagnostic.Kind.ERROR


const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        ToNativeClass::class.java.canonicalName,
        ToNativeInterface::class.java.canonicalName,
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

        val generatedInterfaces = roundEnv.getElementsAnnotatedWith(ToNativeInterface::class.java)
            .map { element ->
                generateInterface(
                    element = element,
                    classInspector = classInspector,
                    targetDir = kaptGeneratedDir
                )
            }
            .toMap()

        roundEnv.getElementsAnnotatedWith(ToNativeClass::class.java)
            .forEach { element ->
                generateWrappedClasses(
                    element = element,
                    classInspector = classInspector,
                    generatedInterfaces = generatedInterfaces,
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
    private fun generateInterface(
        element: Element,
        classInspector: ClassInspector,
        targetDir: String
    ): Pair<TypeName, GeneratedSuperInterface> {

        val typeName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toImmutableKmClass().toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeInterface::class.java)
        val newTypeName = annotation.name.nonEmptyOr("${typeName.simpleName}NativeProtocol")

        val generatedType = WrapperInterfaceBuilder(newTypeName, typeSpec).build()

        FileSpec.builder(typeName.packageName, newTypeName)
            .addType(generatedType)
            .build()
            .writeTo(File(targetDir))

        val newInterfaceName = ClassName(typeName.packageName, newTypeName)

        return typeName to GeneratedSuperInterface(newInterfaceName, generatedType)

    }

    @KotlinPoetMetadataPreview
    private fun generateWrappedClasses(
        element: Element,
        classInspector: ClassInspector,
        generatedInterfaces: Map<TypeName, GeneratedSuperInterface>,
        scopeProviders: Map<ClassName, PropertySpec>,
        kaptGeneratedDir: String
    ) {

        val originalTypeName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toImmutableKmClass().toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeClass::class.java)

        val generatedClassName =
            annotation.name.nonEmptyOr("${originalTypeName.simpleName}Native")

        val originalToGeneratedInterfaceName: Pair<TypeName, GeneratedSuperInterface>? =
            matchGeneratedInterfaceName(
                superInterfacesOfClass = typeSpec.superinterfaces.keys,
                className = originalTypeName,
                allGeneratedInterfaces = generatedInterfaces
            )

        val classToGenerateSpec = WrapperClassBuilder(
            wrappedClassName = originalTypeName,
            poetMetadataSpec = typeSpec,
            newTypeName = generatedClassName,
            generatedSuperInterface = originalToGeneratedInterfaceName,
            scopeProviderSpec = obtainScopeProviderSpec(annotation, scopeProviders)
        ).build()

        FileSpec.builder(originalTypeName.packageName, generatedClassName)
            .addType(classToGenerateSpec)
            .build()
            .writeTo(File(kaptGeneratedDir))

    }

    private fun matchGeneratedInterfaceName(
        superInterfacesOfClass: Set<TypeName>,
        className: TypeName,
        allGeneratedInterfaces: Map<TypeName, GeneratedSuperInterface>
    ): Pair<TypeName, GeneratedSuperInterface>? = mutableSetOf<TypeName>()
        .apply {
            addAll(superInterfacesOfClass)
            add(className)
        }
        .find { allGeneratedInterfaces[it] != null }
        ?.let { originalName ->
            originalName to allGeneratedInterfaces[originalName]!!
        }

    private fun obtainScopeProviderSpec(
        annotation: ToNativeClass,
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
        if (typeMirror != null
            && typeMirror.toString() != "com.futuremind.iossuspendwrapper.NoScopeProvider"
            && scopeProviders[typeMirror.asTypeName()] == null
        ) {
            throw IllegalStateException("$typeMirror can only be used in @ToNativeClass(launchOnScope) if it has been annotated with @ExportedScopeProvider")
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
            throw IllegalStateException("ExportedScopeProvider can only be applied to a class extending ScopeProvider interface")
        }
    }

}

data class GeneratedSuperInterface(val name: TypeName, val typeSpec: TypeSpec)