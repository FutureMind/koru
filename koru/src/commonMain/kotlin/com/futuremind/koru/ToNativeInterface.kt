package com.futuremind.koru


/**
 * @param name: The name of the generated wrapper interface. By default it will be called OriginalInterfaceNameNativeProtocol.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ToNativeInterface(
    val name: String = ""
)