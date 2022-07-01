package com.futuremind.koru.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName

fun KSClassDeclaration.toTypeSpec(): TypeSpec {
    val builder = when (this.isAbstract()) {
        true -> TypeSpec.interfaceBuilder(this.toClassName())
        false -> TypeSpec.classBuilder(this.toClassName())
    }

    return builder
        .addModifiers(modifiers.map { it.toKModifier()!! })
        .addSuperinterfaces(
            superTypes.toList().map { it.toTypeName() }
        )
        .addFunctions(
            getDeclaredFunctions()
                .filterNot { it.isConstructor() }
                .toList()
                .map {
                    println("A: ${it.returnType}")
                    println("B: ${it.returnType?.toTypeName()}")
                    it.toFunSpec()
                }
        )
        .addProperties(
            getDeclaredProperties()
                .toList()
                .map { it.toPropertySpec() }
        )
        .build()
}

private fun KSFunctionDeclaration.toFunSpec(): FunSpec =
    FunSpec.builder(simpleName.asString())
        .addModifiers(modifiers.map { it.toKModifier()!! })
        .addParameters(parameters.map { it.toParameterSpec() })
        .returns(returnType!!.toTypeName())
        .build()

private fun KSValueParameter.toParameterSpec() = ParameterSpec.builder(
    name = this.name!!.getShortName(),
    type = this.type.toTypeName()
).build()

private fun KSPropertyDeclaration.toPropertySpec(): PropertySpec =
    PropertySpec.builder(simpleName.asString(), type.toTypeName())
        .addModifiers(modifiers.map { it.toKModifier()!! })
        .build()