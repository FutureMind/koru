package com.futuremind.iossuspendwrapper.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.util.Locale
import com.futuremind.iossuspendwrapper.ScopeProvider


/**
 * Generates a top level property [ScopeProvider] exposing a CoroutineScope to be injected into
 * generated iOS classes via @ToNativeClass(launchOnScope = ...).
 */
class ScopeProviderBuilder(
    packageName: String,
    poetMetadataSpec: TypeSpec
) {

    private val scopeProviderClassName = ClassName(packageName, poetMetadataSpec.name.toString())
    private val scopePropertyName = "exportedScopeProvider_"+poetMetadataSpec.name!!.decapitalize(Locale.ROOT)

    fun build(): PropertySpec = PropertySpec
        .builder(scopePropertyName, scopeProviderClassName, KModifier.PUBLIC)
        .initializer("%T()", scopeProviderClassName)
        .build()

}
