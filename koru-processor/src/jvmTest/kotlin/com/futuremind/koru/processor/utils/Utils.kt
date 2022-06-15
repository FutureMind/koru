package com.futuremind.koru.processor.utils

import kotlin.reflect.KClass

fun KClass<*>.member(methodName: String) =
    members.find { it.name == methodName }!!

fun KClass<*>.memberReturnType(methodName: String) = member(methodName).returnType.toString()

const val defaultClassNameSuffix = "Native"
const val defaultInterfaceNameSuffix = "NativeProtocol"
