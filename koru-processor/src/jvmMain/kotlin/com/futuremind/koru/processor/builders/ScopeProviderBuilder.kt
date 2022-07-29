package com.futuremind.koru.processor.builders

import com.futuremind.koru.ScopeProvider
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec


/**
 * Generates a top level property [ScopeProvider] exposing a CoroutineScope to be injected into
 * generated native classes via @ToNativeClass(launchOnScope = ...).
 */
class ScopeProviderBuilder(
    private val scopeProviderClassName: ClassName,
    private val scopePropertyName: String
) {

    fun build(): PropertySpec = PropertySpec
        .builder(scopePropertyName, scopeProviderClassName, KModifier.PUBLIC)
        .initializer("%T()", scopeProviderClassName)
        .build()

}
