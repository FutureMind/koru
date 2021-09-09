package com.futuremind.koru.processor

import com.futuremind.koru.FlowWrapper
import com.futuremind.koru.ScopeProvider
import com.futuremind.koru.SuspendWrapper
import com.squareup.kotlinpoet.*


class WrapperClassBuilder(
    originalTypeName: ClassName,
    originalTypeSpec: TypeSpec,
    generatedInterfaces: Map<TypeName, GeneratedInterface>,
    private val newTypeName: String,
    private val scopeProviderMemberName: MemberName?,
    private val freezeWrapper: Boolean
) : WrapperBuilder(originalTypeName, originalTypeSpec, generatedInterfaces) {

    companion object {
        private const val WRAPPED_PROPERTY_NAME = "wrapped"
        private const val SCOPE_PROVIDER_PROPERTY_NAME = "scopeProvider"
    }

    private val constructorSpec = FunSpec
        .constructorBuilder()
        .addParameter(WRAPPED_PROPERTY_NAME, originalTypeName)
        .addParameter(
            ParameterSpec
                .builder(
                    SCOPE_PROVIDER_PROPERTY_NAME,
                    ScopeProvider::class.asTypeName().copy(nullable = true)
                )
                .build()
        )
        .apply {
            if (freezeWrapper) {
                this.addStatement(
                    "this.%M()",
                    MemberName("com.futuremind.koru", "freeze")
                )
            }
        }
        .build()

    private val secondaryConstructorSpec = FunSpec
        .constructorBuilder()
        .addParameter(WRAPPED_PROPERTY_NAME, originalTypeName)
        .callThisConstructor(
            buildCodeBlock {
                add("%N", WRAPPED_PROPERTY_NAME)
                add(",")
                when (scopeProviderMemberName) {
                    null -> add("null")
                    else -> add("%M", scopeProviderMemberName)
                }
            }
        )
        .build()

    private val wrappedClassPropertySpec = PropertySpec
        .builder(WRAPPED_PROPERTY_NAME, originalTypeName)
        .initializer(WRAPPED_PROPERTY_NAME)
        .addModifiers(KModifier.PRIVATE)
        .build()

    private val scopeProviderPropertySpec = PropertySpec
        .builder(
            SCOPE_PROVIDER_PROPERTY_NAME,
            ScopeProvider::class.asTypeName().copy(nullable = true)
        )
        .initializer(SCOPE_PROVIDER_PROPERTY_NAME)
        .addModifiers(KModifier.PRIVATE)
        .build()

    private val functions = originalTypeSpec.funSpecs
        .filter { !it.modifiers.contains(KModifier.PRIVATE) }
        .map { originalFuncSpec ->
            originalFuncSpec.toBuilder(name = originalFuncSpec.name)
                .clearBody()
                .setFunctionBody(originalFuncSpec)
                .setReturnType(originalFuncSpec)
                .apply {
                    modifiers.remove(KModifier.SUSPEND)
                    modifiers.remove(KModifier.ABSTRACT) //when we create class, we always wrap into a concrete impl
                    when (originalFuncSpec.overridesGeneratedInterface()) {
                        true -> this.modifiers.add(KModifier.OVERRIDE)
                        false -> this.modifiers.remove(KModifier.OVERRIDE)
                    }
                }
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
                .getter(
                    FunSpec.getterBuilder()
                        .setGetterBody(originalPropertySpec)
                        .build()
                )
                .mutable(false)
                .apply {
                    modifiers.remove(KModifier.ABSTRACT)
                    when (originalPropertySpec.overridesGeneratedInterface()) {
                        true -> this.modifiers.add(KModifier.OVERRIDE)
                        false -> this.modifiers.remove(KModifier.OVERRIDE)
                    }
                }
                .build()
        }

    private val modifiers: Set<KModifier> = originalTypeSpec.modifiers()

    /**
     * if we have an interface generated based on class signature, we need to add the override
     * modifier to its methods explicitly
     */
    private fun FunSpec.overridesGeneratedInterface(): Boolean {

        //not comparing types because we're comparing koru-wrapped interface with original
        fun FunSpec.hasSameSignature(other: FunSpec) =
            this.name == other.name && this.parameters == other.parameters

        fun TypeSpec.containsFunctionSignature() =
            this.funSpecs.any { it.hasSameSignature(this@overridesGeneratedInterface) }

        return superInterfaces.any { it.typeSpec.containsFunctionSignature() }
    }

    /**
     * if we have an interface generated based on class signature, we need to add the override
     * modifier to its properties explicitly.
     */
    private fun PropertySpec.overridesGeneratedInterface(): Boolean {

        //not comparing types because we're comparing koru-wrapped interface with original
        fun PropertySpec.hasSameSignature(other: PropertySpec) = this.name == other.name

        fun TypeSpec.containsPropertySignature() =
            this.propertySpecs.any { it.hasSameSignature(this@overridesGeneratedInterface) }

        return superInterfaces.any { it.typeSpec.containsPropertySignature() }
    }

    //this could be simplified in the future, but for now: https://github.com/square/kotlinpoet/issues/966
    private fun FunSpec.Builder.setFunctionBody(originalFunSpec: FunSpec): FunSpec.Builder = when {
        originalFunSpec.isSuspend -> wrapOriginalSuspendFunction(originalFunSpec)
        originalFunSpec.returnType.isFlow -> wrapOriginalFlowFunction(originalFunSpec)
        else -> callOriginalBlockingFunction(originalFunSpec)
    }

    private fun FunSpec.Builder.setGetterBody(originalPropSpec: PropertySpec): FunSpec.Builder {
        val getterInvocation = when {
            originalPropSpec.type.isFlow -> flowWrapperFunctionBody(originalPropSpec.asInvocation()).toString()
            else -> "return ${originalPropSpec.asInvocation()}"
        }
        return this.addStatement(getterInvocation)
    }

    /** E.g. return SuspendWrapper(mainScopeProvider) { doSth(whatever) }*/
    private fun FunSpec.Builder.wrapOriginalSuspendFunction(
        originalFunSpec: FunSpec
    ): FunSpec.Builder = addCode(
        buildCodeBlock {
            add("return %T(", SuspendWrapper::class)
            add(SCOPE_PROVIDER_PROPERTY_NAME)
            add(", ")
            add("%L", freezeWrapper)
            add(") ")
            add("{ ${originalFunSpec.asInvocation()} }")
        }
    )

    /** E.g. return FlowWrapper(mainScopeProvider, doSth(whatever)) */
    private fun FunSpec.Builder.wrapOriginalFlowFunction(
        originalFunSpec: FunSpec
    ): FunSpec.Builder = addCode(
        flowWrapperFunctionBody(originalFunSpec.asInvocation())
    )

    private fun flowWrapperFunctionBody(callOriginal: String) = buildCodeBlock {
        add("return %T(", FlowWrapper::class)
        add(SCOPE_PROVIDER_PROPERTY_NAME)
        add(", %L", freezeWrapper)
        add(", ${callOriginal})")
    }

    private fun FunSpec.Builder.callOriginalBlockingFunction(originalFunSpec: FunSpec): FunSpec.Builder =
        this.addStatement("return ${originalFunSpec.asInvocation()}")

    private fun FunSpec.asInvocation(): String {
        val paramsDeclaration = parameters.joinToString(", ") { it.name }
        return "${WRAPPED_PROPERTY_NAME}.${this.name}($paramsDeclaration)"
    }

    private fun PropertySpec.asInvocation(): String {
        return "${WRAPPED_PROPERTY_NAME}.${this.name}"
    }

    fun build(): TypeSpec = TypeSpec
        .classBuilder(newTypeName)
        .addModifiers(modifiers)
        .addSuperinterfaces(superInterfacesNames)
        .primaryConstructor(constructorSpec)
        .addFunction(secondaryConstructorSpec)
        .addProperty(wrappedClassPropertySpec)
        .addProperty(scopeProviderPropertySpec)
        .addProperties(properties)
        .addFunctions(functions)
        .build()

}
