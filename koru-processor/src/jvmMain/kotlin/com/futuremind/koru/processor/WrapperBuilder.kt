package com.futuremind.koru.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec


abstract class WrapperBuilder(
    originalTypeName: ClassName,
    originalTypeSpec: TypeSpec,
    private val generatedInterfaces: Map<TypeName, GeneratedInterface>,
) {

    protected fun TypeSpec.modifiers() = modifiers.let {
        if (it.contains(KModifier.PRIVATE)) throw IllegalStateException("Cannot wrap types with `private` modifier. Consider using internal or public.")
        it.ifEmpty { setOf(KModifier.PUBLIC) }
    }

    /**
     * 1. Add generated standalone superinterfaces if they match the original superinterfaces.
     * 2. Also add the interface generated from this class if it exists.
     */
    protected val superInterfaces: List<GeneratedInterface> = originalTypeSpec.superinterfaces.keys
        .toMutableList()
        .apply { add(originalTypeName) }
        .mapNotNull { interfaceName ->
            when (val matchingSuper = generatedInterfaces[interfaceName]) {
                null -> null
                else -> matchingSuper
            }
        }

    protected val superInterfacesNames = superInterfaces.map { it.name }

}