package com.futuremind.koru.processor

fun wrongScopeProviderSupertype(): Nothing =
    throw IllegalStateException("ExportedScopeProvider can only be applied to a class extending ScopeProvider interface")
