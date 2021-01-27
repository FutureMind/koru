package com.futuremind.iossuspendwrapper.processor

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.flow.Flow
import com.futuremind.iossuspendwrapper.SuspendWrapper
import com.futuremind.iossuspendwrapper.FlowWrapper
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy


fun FunSpec.Builder.addReturnStatement(returnType: TypeName?): FunSpec.Builder = when {
    this.isSuspend -> this.returns(
        SuspendWrapper::class.asTypeName().parameterizedBy(returnType.orUnit)
    )
    returnType.isFlow -> this.returns(
        FlowWrapper::class.asTypeName().parameterizedBy(returnType.flowGenericType)
    )
    else -> this.returns(returnType.orUnit)
}

val TypeName?.isFlow: Boolean
    get() = (this as? ParameterizedTypeName)?.rawType == Flow::class.asTypeName()

private val TypeName?.flowGenericType: TypeName
    get() = (this as? ParameterizedTypeName)?.typeArguments?.get(0)
        ?: throw IllegalStateException("Should only be called on Flow TypeName")

val FunSpec.Builder.isSuspend: Boolean
    get() = this.modifiers.contains(KModifier.SUSPEND)

private val TypeName?.orUnit
    get() = this ?: Unit::class.asTypeName()