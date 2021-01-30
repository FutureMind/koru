package com.futuremind.koru


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ToNativeInterface(
    val name: String = ""
)