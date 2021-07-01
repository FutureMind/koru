package com.futuremind.koru

import kotlin.reflect.KClass


/**
 * @param name: The name of the generated wrapper class. By default it will be called OriginalClassNameNative.
 * @param freeze: Whether to freeze the wrapper class, the SuspendWrapper/FlowWrapper and the job
 *      they produce. If freeze=false (default) then the wrapped class can be mutable. However if
 *      you want to pass the wrapper across threads, you need to have it frozen and that's when
 *      freeze=true comes in handy.
 * @param launchOnScope: The ScopeProvider which gives scope for launching the coroutines inside
 *      wrapper. Can be omitted, but then the scope needs to be passed directly to the wrapper
 *      inside iOS code. Providing it here deals with the inconvenience.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ToNativeClass(
    val name: String = "",
    val freeze: Boolean = false,
    val launchOnScope: KClass<out ScopeProvider> = NoScopeProvider::class
)

class NoScopeProvider : ScopeProvider {
    override val scope = throw RuntimeException("This is a dummy no-op ScopeProvider")
}
