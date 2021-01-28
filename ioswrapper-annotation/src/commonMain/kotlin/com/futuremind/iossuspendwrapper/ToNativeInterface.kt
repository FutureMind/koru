package com.futuremind.iossuspendwrapper


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ToNativeInterface(
    val name: String = ""
)