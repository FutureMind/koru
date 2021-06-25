package com.futuremind.koru

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*


class FlowWrapper<T>(
    private val scopeProvider: ScopeProvider?,
    private val flow: Flow<T>
) {

    fun subscribe(
        onEach: (item: T) -> Unit,
        onComplete: () -> Unit,
        onThrow: (error: Throwable) -> Unit
    ) = subscribe(
        scope = scopeProvider?.scope,
        onEach = onEach,
        onComplete = onComplete,
        onThrow = onThrow
    )

    fun subscribe(
        scope: CoroutineScope?,
        onEach: (item: T) -> Unit,
        onComplete: () -> Unit,
        onThrow: (error: Throwable) -> Unit
    ): Job? {
        if(scope==null){
            onThrow(IllegalArgumentException("To use implicit scope, you have to provide it via @ToNativeClass.launchOnScope and @ExportedScopeProvider."))
            return null
        }
        return flow
            .onEach { onEach(it.freeze()) }
            .catch { onThrow(it.freeze()) }
            .onCompletion { onComplete() }
            .launchIn(scope)
    }
}