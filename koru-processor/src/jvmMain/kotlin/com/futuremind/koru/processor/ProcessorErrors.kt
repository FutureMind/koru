package com.futuremind.koru.processor

import com.squareup.kotlinpoet.TypeName

fun wrongScopeProviderSupertype(): Nothing =
    throw IllegalStateException("ExportedScopeProvider can only be applied to a class extending ScopeProvider interface")

fun requiredExportOfScopeProvider(scopeProvider: TypeName): Nothing =
    throw IllegalStateException("$scopeProvider can only be used in @ToNativeClass(launchOnScope) if it has been annotated with @ExportedScopeProvider")
