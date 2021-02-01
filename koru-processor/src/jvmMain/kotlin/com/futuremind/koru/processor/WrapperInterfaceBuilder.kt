package com.futuremind.koru.processor

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec


class WrapperInterfaceBuilder(
    private val newTypeName: String,
    poetMetadataSpec: TypeSpec
) {

    private val functions = poetMetadataSpec.funSpecs
        .map { originalFuncSpec ->
            originalFuncSpec.toBuilder(name = originalFuncSpec.name)
                .clearBody()
                .addReturnStatement(originalFuncSpec.returnType)
                .apply {
                    modifiers.remove(KModifier.SUSPEND)
                    modifiers.add(KModifier.ABSTRACT)
                }
                .build()
        }

    fun build(): TypeSpec = TypeSpec
        .interfaceBuilder(newTypeName)
        .addFunctions(functions)
        .build()

}
