package com.futuremind.koru.processor

import com.futuremind.koru.*
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
import javax.lang.model.type.MirroredTypeException
import javax.tools.Diagnostic.Kind.ERROR


const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class KaptProcessor : AbstractProcessor() {

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

        val generatedInterfaces = mutableMapOf<TypeName, GeneratedInterface>()

        roundEnv.getElementsAnnotatedWith(ToNativeInterface::class.java)
            .sortByInheritance(classInspector, processingEnv)
            .forEach { element ->
                val (typeName, generatedInterface) = generateInterface(
                    element = element,
                    classInspector = classInspector,
                    generatedInterfaces = generatedInterfaces,
                    targetDir = kaptGeneratedDir
                )
                generatedInterfaces[typeName] = generatedInterface
            }

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
        e.printStackTrace()
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
        val scopeClassSpec = (element as TypeElement).toTypeSpec(classInspector)

        scopeClassSpec.assertExtendsScopeProvider()

        val originalClassName = element.getClassName(processingEnv)
        val scopeProviderClassName = ClassName(packageName, scopeClassSpec.name.toString())
        val scopePropertyName = "exportedScopeProvider_"+ scopeClassSpec.name!!.replaceFirstChar { it.lowercase(Locale.ROOT) }
        val propertySpec = ScopeProviderBuilder(
            scopeProviderClassName,
            scopePropertyName
        ).build()

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
        generatedInterfaces: Map<TypeName, GeneratedInterface>,
        targetDir: String
    ): Pair<TypeName, GeneratedInterface> {

        val typeName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeInterface::class.java)
        val newTypeName = annotation.name.nonEmptyOr("${typeName.simpleName}NativeProtocol")

        val generatedType =
            WrapperInterfaceBuilder(typeName, typeSpec, newTypeName, generatedInterfaces).build()

        FileSpec.builder(typeName.packageName, newTypeName)
            .addType(generatedType)
            .build()
            .writeTo(File(targetDir))

        val newInterfaceName = ClassName(typeName.packageName, newTypeName)

        return typeName to GeneratedInterface(newInterfaceName, generatedType)

    }

    @KotlinPoetMetadataPreview
    private fun generateWrappedClasses(
        element: Element,
        classInspector: ClassInspector,
        generatedInterfaces: Map<TypeName, GeneratedInterface>,
        scopeProviders: Map<ClassName, PropertySpec>,
        kaptGeneratedDir: String
    ) {

        val originalTypeName = element.getClassName(processingEnv)
        val typeSpec = (element as TypeElement).toTypeSpec(classInspector)
        val annotation = element.getAnnotation(ToNativeClass::class.java)

        val generatedClassName =
            annotation.name.nonEmptyOr("${originalTypeName.simpleName}Native")

        val classToGenerateSpec = WrapperClassBuilder(
            originalTypeName = originalTypeName,
            originalTypeSpec = typeSpec,
            newTypeName = generatedClassName,
            generatedInterfaces = generatedInterfaces,
            scopeProviderMemberName = obtainScopeProviderMemberName(annotation, scopeProviders),
            freezeWrapper = annotation.freeze
        ).build()

        FileSpec.builder(originalTypeName.packageName, generatedClassName)
            .addType(classToGenerateSpec)
            .build()
            .writeTo(File(kaptGeneratedDir))

    }

    private fun obtainScopeProviderMemberName(
        annotation: ToNativeClass,
        availableScopeProviders: Map<ClassName, PropertySpec>
    ): MemberName? {
        //this is the dirtiest hack ever but it works :O
        //there probably is some way of doing this via kotlinpoet-metadata
        //https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
        var scopeProviderTypeName: TypeName? = null
        try {
            annotation.launchOnScope
        } catch (e: MirroredTypeException) {
            scopeProviderTypeName = e.typeMirror.asTypeName()
        }
        if (scopeProviderTypeName != null
            && scopeProviderTypeName != NoScopeProvider::class.asTypeName()
            && availableScopeProviders[scopeProviderTypeName] == null
        ) {
            throw IllegalStateException("$scopeProviderTypeName can only be used in @ToNativeClass(launchOnScope) if it has been annotated with @ExportedScopeProvider")
        }
        return availableScopeProviders[scopeProviderTypeName]?.let {
            MemberName(
                packageName = (scopeProviderTypeName as ClassName).packageName,
                simpleName = it.name
            )
        }
    }

    private fun String.nonEmptyOr(or: String) = when (this.isEmpty()) {
        true -> or
        false -> this
    }

    private fun TypeSpec.assertExtendsScopeProvider() {
        if (!superinterfaces.contains(ScopeProvider::class.asTypeName())) {
            throw IllegalStateException("ExportedScopeProvider can only be applied to a class extending ScopeProvider interface")
        }
    }

}

internal fun Element.getPackage(processingEnv: ProcessingEnvironment) =
    processingEnv.elementUtils.getPackageOf(this).toString()

internal fun Element.getClassName(processingEnv: ProcessingEnvironment) =
    ClassName(this.getPackage(processingEnv), this.simpleName.toString())

data class GeneratedInterface(val name: TypeName, val typeSpec: TypeSpec)
