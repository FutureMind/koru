package com.futuremind.koru.processor

import com.squareup.kotlinpoet.*


abstract class WrapperBuilder(
    originalTypeName: ClassName,
    originalTypeSpec: TypeSpec,
    private val generatedInterfaces: Map<TypeName, GeneratedInterface>,
) {

    protected val modifiers: Set<KModifier> = originalTypeSpec.modifiers.let {
        if (it.contains(KModifier.PRIVATE)) throw IllegalStateException("Cannot wrap types with `private` modifier. Consider using internal or public.")
        it.ifEmpty { setOf(KModifier.PUBLIC) }
    }

    /**
     * 1. Add generated standalone superinterfaces if they match the original superinterfaces.
     * 2. Also add the interface generated from this class if it exists.
     */
    private val superInterfaces: List<GeneratedInterface> = originalTypeSpec.superinterfaces.keys
        .toMutableList()
        .apply { add(originalTypeName) }
        .mapNotNull { interfaceName ->
            when (val matchingSuper = generatedInterfaces[interfaceName]) {
                null -> null
                else -> matchingSuper
            }
        }

    protected val superInterfacesNames = superInterfaces.map { it.name }

    protected fun FunSpec.Builder.setupOverrideModifier(originalFuncSpec: FunSpec) = apply {
        when (originalFuncSpec.overridesGeneratedInterface()) {
            true -> this.modifiers.add(KModifier.OVERRIDE)
            false -> this.modifiers.remove(KModifier.OVERRIDE)
        }
    }

    protected fun PropertySpec.Builder.setupOverrideModifier(originalPropertySpec: PropertySpec) = apply {
        when (originalPropertySpec.overridesGeneratedInterface()) {
            true -> this.modifiers.add(KModifier.OVERRIDE)
            false -> this.modifiers.remove(KModifier.OVERRIDE)
        }
    }

    private fun FunSpec.overridesGeneratedInterface(): Boolean {

        //not comparing types because we're comparing koru-wrapped interface with original
        fun FunSpec.hasSameSignature(other: FunSpec) =
            this.name == other.name && this.parameters == other.parameters

        fun TypeSpec.containsFunctionSignature() =
            this.funSpecs.any { it.hasSameSignature(this@overridesGeneratedInterface) }

        return superInterfaces.any { it.typeSpec.containsFunctionSignature() }
    }

    private fun PropertySpec.overridesGeneratedInterface(): Boolean {

        //not comparing types because we're comparing koru-wrapped interface with original
        fun PropertySpec.hasSameSignature(other: PropertySpec) = this.name == other.name

        fun TypeSpec.containsPropertySignature() =
            this.propertySpecs.any { it.hasSameSignature(this@overridesGeneratedInterface) }

        return superInterfaces.any { it.typeSpec.containsPropertySignature() }
    }

}