package com.futuremind.koru.processor

import com.futuremind.koru.NoScopeProvider
import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.squareup.kotlinpoet.*
import java.util.Locale

fun interfaceName(
    annotation: ToNativeInterface,
    originalTypeName: String
) = annotation.name.ifEmpty { "${originalTypeName}NativeProtocol" }

fun className(
    annotation: ToNativeClass,
    originalTypeName: String
) = annotation.name.ifEmpty { "${originalTypeName}Native" }

fun scopeProviderContainerName(
    generatedProperty: GeneratedPropertySpec
) = "${generatedProperty.newTypeName.simpleName}Container"

fun scopeProviderPropertyName(
    scopeProviderClassName: ClassName
) = "exportedScopeProvider_" + scopeProviderClassName.simpleName.replaceFirstChar { it.lowercase(Locale.ROOT) }

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
