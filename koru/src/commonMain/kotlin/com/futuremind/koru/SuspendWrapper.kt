package com.futuremind.koru

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class SuspendWrapper<T>(
    private val scopeProvider: ScopeProvider?,
    private val suspender: suspend () -> T
) {
    fun subscribe(
        onSuccess: (item: T) -> Unit,
        onThrow: (error: Throwable) -> Unit
    ) = subscribe(
        scope = scopeProvider?.scope,
        onSuccess = onSuccess,
        onThrow = onThrow
    )

    fun subscribe(
        scope: CoroutineScope?,
        onSuccess: (item: T) -> Unit,
        onThrow: (error: Throwable) -> Unit
    ): Job?  {
        if(scope==null){
            onThrow(IllegalArgumentException("To use implicit scope, you have to provide it via @ToNativeClass.launchOnScope and @ExportedScopeProvider."))
        }
        return scope?.launch {
            try {
                onSuccess(suspender().freeze())
            } catch (error: Throwable) {
                onThrow(error.freeze())
            }
        } 
    } 
}
