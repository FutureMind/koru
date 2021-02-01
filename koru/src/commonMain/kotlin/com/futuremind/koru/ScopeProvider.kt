package com.futuremind.koru

import kotlinx.coroutines.CoroutineScope

interface ScopeProvider {
    val scope: CoroutineScope
}