package com.futuremind.iossuspendwrapper.processor

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec


/**
 * (1) Either generates an iOS counterpart for an existing interface via @WrapForIos annotated
 * interface...
 *
 * (2) ...or generates an iOS interface based on an existing class (2) via
 * @WrapForIos(generateInterface = true) annotated class.
 */
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
