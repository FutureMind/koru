package com.futuremind.iossuspendwrapper

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class WrapForIos(
    val className: String = "",
    val generateInterface: Boolean = false,
    val generatedInterfaceName: String = "",
    val launchOnScope: KClass<out ScopeProvider> = NoScopeProvider::class
)

class NoScopeProvider : ScopeProvider {
    override val scope = throw RuntimeException("This is a dummy no-op ScopeProvider")
}

/* TODO
    - annotation should have their own class, WrapForIos no longer makes sense for them with those params
    - freezeClass: Boolean = false - adds com.futuremind.iossuspendwrapper.freeze to init block of the class (low prio)
 */