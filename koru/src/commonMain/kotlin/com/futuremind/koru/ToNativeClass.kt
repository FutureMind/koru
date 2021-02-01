package com.futuremind.koru

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ToNativeClass(
    val name: String = "",
    val launchOnScope: KClass<out ScopeProvider> = NoScopeProvider::class
)

class NoScopeProvider : ScopeProvider {
    override val scope = throw RuntimeException("This is a dummy no-op ScopeProvider")
}

/* TODO
    - freezeClass: Boolean = false - adds com.futuremind.koru.freeze to init block of the class (low prio)
 */