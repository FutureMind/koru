package com.futuremind.koru.processor

import com.futuremind.koru.NoScopeProvider
import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName

fun interfaceName(
    annotation: ToNativeInterface,
    originalTypeName: String
) = annotation.name.ifEmpty { "${originalTypeName}NativeProtocol" }

fun className(
    annotation: ToNativeClass,
    originalTypeName: String
) = annotation.name.ifEmpty { "${originalTypeName}Native" }

fun findMatchingScopeProvider(
    scopeProviderTypeName: TypeName?,
    availableScopeProviders: Map<ClassName, PropertySpec>
): MemberName? {
    if (scopeProviderTypeName == NoScopeProvider::class.asTypeName()) return null
    if (scopeProviderTypeName != null && availableScopeProviders[scopeProviderTypeName] == null) {
        requiredExportOfScopeProvider(scopeProviderTypeName)
    }
    return availableScopeProviders[scopeProviderTypeName]?.let {
        MemberName(
            packageName = (scopeProviderTypeName as ClassName).packageName,
            simpleName = it.name
        )
    }
}

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
fun ToNativeClass.launchOnScopeTypeName(): TypeName? {
    //this is the dirtiest hack ever but it works :O
    //https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
    var scopeProviderTypeName: TypeName? = null
    try {
        println("Annotation name: $name")
        this.launchOnScope
//    } catch (e: MirroredTypeException) {
//        scopeProviderTypeName = e.typeMirror.asTypeName()
    } catch (e: KSTypeNotPresentException){
        scopeProviderTypeName = e.ksType.toTypeName()
    }
    if (scopeProviderTypeName == NoScopeProvider::class.asTypeName()) return null
    return scopeProviderTypeName
}
