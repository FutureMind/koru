package com.futuremind.iossuspendwrapper

import kotlinx.coroutines.CoroutineScope

interface ScopeProvider {
    val scope: CoroutineScope
}