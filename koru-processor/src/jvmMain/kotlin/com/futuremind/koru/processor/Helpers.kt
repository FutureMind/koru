package com.futuremind.koru.processor

import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

fun interfaceName(
    annotation: ToNativeInterface,
    originalTypeName: String
) = annotation.name.nonEmptyOr("${originalTypeName}NativeProtocol")

fun className(
    annotation: ToNativeClass,
    originalTypeName: String
) = annotation.name.nonEmptyOr("${originalTypeName}Native")

fun findMatchingScopeProvider(
    scopeProviderTypeName: TypeName?,
    availableScopeProviders: Map<ClassName, PropertySpec>
): MemberName? {
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

private fun String.nonEmptyOr(or: String) = this.ifEmpty { or }
