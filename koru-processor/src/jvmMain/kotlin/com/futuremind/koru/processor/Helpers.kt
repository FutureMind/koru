package com.futuremind.koru.processor

import com.futuremind.koru.ToNativeClass
import com.futuremind.koru.ToNativeInterface

fun interfaceName(
    annotation: ToNativeInterface,
    originalTypeName: String
) = annotation.name.nonEmptyOr("${originalTypeName}NativeProtocol")

fun className(
    annotation: ToNativeClass,
    originalTypeName: String
) = annotation.name.nonEmptyOr("${originalTypeName}Native")

private fun String.nonEmptyOr(or: String) = this.ifEmpty { or }
