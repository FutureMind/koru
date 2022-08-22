package com.futuremind.koru.processor.builders

import com.futuremind.koru.processor.GeneratedInterfaceSpec
import com.squareup.kotlinpoet.*


class WrapperInterfaceBuilder(
    originalTypeName: ClassName,
    originalTypeSpec: TypeSpec,
    private val newTypeName: String,
    generatedInterfaces: Map<TypeName, GeneratedInterfaceSpec>,
) : WrapperBuilder(originalTypeName, originalTypeSpec, generatedInterfaces) {

    private val functions = originalTypeSpec.funSpecs
        .filter { !it.modifiers.contains(KModifier.PRIVATE) }
        .map { originalFuncSpec ->
            originalFuncSpec.toBuilder(name = originalFuncSpec.name)
                .clearBody()
                .setReturnType(originalFuncSpec)
                .apply {
                    modifiers.add(KModifier.ABSTRACT)
                    modifiers.remove(KModifier.SUSPEND)
                    modifiers.remove(KModifier.ACTUAL)
                    modifiers.remove(KModifier.EXPECT)
                }
                .setupOverrideModifier(originalFuncSpec)
                .build()
        }

    private val properties = originalTypeSpec.propertySpecs
        .filter { !it.modifiers.contains(KModifier.PRIVATE) }
        .map { originalPropertySpec ->
            PropertySpec
                .builder(
                    name = originalPropertySpec.name,
                    type = originalPropertySpec.wrappedType
                )
                .mutable(false)
                .apply {
                    modifiers.add(KModifier.ABSTRACT)
                    modifiers.remove(KModifier.ACTUAL)
                    modifiers.remove(KModifier.EXPECT)
                }
                .setupOverrideModifier(originalPropertySpec)
                .build()
        }

    fun build(): TypeSpec = TypeSpec
        .interfaceBuilder(newTypeName)
        .addModifiers(modifiers)
        .addSuperinterfaces(superInterfacesNames)
        .addFunctions(functions)
        .addProperties(properties)
        .build()

}
