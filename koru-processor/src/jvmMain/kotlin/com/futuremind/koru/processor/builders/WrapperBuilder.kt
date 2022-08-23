package com.futuremind.koru.processor.builders

import com.futuremind.koru.processor.GeneratedInterfaceSpec
import com.squareup.kotlinpoet.*


abstract class WrapperBuilder(
    originalTypeName: ClassName,
    originalTypeSpec: TypeSpec,
    private val generatedInterfaces: Map<TypeName, GeneratedInterfaceSpec>,
) {

    protected val modifiers: Set<KModifier> = originalTypeSpec.modifiers.let { originalModifiers ->
        if (originalModifiers.isPrivateOrProtected()) throw IllegalStateException("Cannot wrap types with `private` modifier. Consider using internal or public.")
        originalModifiers
            .toMutableSet()
            .apply {
                //wrapped type is always in the native source set
                remove(KModifier.ACTUAL)
                remove(KModifier.EXPECT)
            }
            .ifEmpty { setOf(KModifier.PUBLIC) }
    }

    /**
     * 1. Add generated standalone superinterfaces if they match the original superinterfaces.
     * 2. Also add the interface generated from this class if it exists.
     */
    private val superInterfaces: List<GeneratedInterfaceSpec> = originalTypeSpec.superinterfaces.keys
        .toMutableList()
        .apply { add(originalTypeName) }
        .mapNotNull { interfaceName ->
            when (val matchingSuper = generatedInterfaces[interfaceName]) {
                null -> null
                else -> matchingSuper
            }
        }

    protected val superInterfacesNames = superInterfaces.map { it.newTypeName }

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

        return superInterfaces.any { it.newSpec.containsFunctionSignature() }
    }

    private fun PropertySpec.overridesGeneratedInterface(): Boolean {

        //not comparing types because we're comparing koru-wrapped interface with original
        fun PropertySpec.hasSameSignature(other: PropertySpec) = this.name == other.name

        fun TypeSpec.containsPropertySignature() =
            this.propertySpecs.any { it.hasSameSignature(this@overridesGeneratedInterface) }

        return superInterfaces.any { it.newSpec.containsPropertySignature() }
    }

    protected fun Collection<KModifier>.isPrivateOrProtected() = contains(KModifier.PRIVATE) || contains(KModifier.PROTECTED)

}