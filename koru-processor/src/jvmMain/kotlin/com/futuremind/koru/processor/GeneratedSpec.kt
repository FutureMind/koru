package com.futuremind.koru.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

data class GeneratedSpec<T>(
    val originalTypeName: TypeName,
    val newTypeName: ClassName,
    val newSpec: T
)

typealias GeneratedClassSpec = GeneratedSpec<TypeSpec>
typealias GeneratedInterfaceSpec = GeneratedSpec<TypeSpec>
typealias GeneratedPropertySpec = GeneratedSpec<PropertySpec>