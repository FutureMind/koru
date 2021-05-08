package com.futuremind.koru.processor

import com.squareup.kotlinpoet.*
import com.futuremind.koru.SuspendWrapper
import com.futuremind.koru.FlowWrapper
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.flow.*


fun FunSpec.Builder.setReturnType(returnType: TypeName?): FunSpec.Builder = when {
    this.isSuspend -> this.returns(
        SuspendWrapper::class.asTypeName().parameterizedBy(returnType.orUnit)
    )
    returnType.isFlow -> this.returns(
        FlowWrapper::class.asTypeName().parameterizedBy(returnType.flowGenericType)
    )
    else -> this.returns(returnType.orUnit)
}

val PropertySpec.wrappedType get() = when {
    type.isFlow -> FlowWrapper::class.asTypeName().parameterizedBy(type.flowGenericType)
    else -> type
}

val TypeName?.isFlow: Boolean
    get() {
        val rawType = (this as? ParameterizedTypeName)?.rawType?.topLevelClassName()
        return rawType == Flow::class.asTypeName()
            || rawType == StateFlow::class.asTypeName()
            || rawType == MutableStateFlow::class.asTypeName()
            || rawType == SharedFlow::class.asTypeName()
            || rawType == MutableSharedFlow::class.asTypeName()
    }

private val TypeName?.flowGenericType: TypeName
    get() = (this as? ParameterizedTypeName)?.typeArguments?.get(0)
        ?: throw IllegalStateException("Should only be called on Flow TypeName")

val FunSpec.Builder.isSuspend: Boolean
    get() = this.modifiers.contains(KModifier.SUSPEND)

private val TypeName?.orUnit
    get() = this ?: Unit::class.asTypeName()