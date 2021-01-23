package com.futuremind.iossuspendwrapper

actual fun <T> T.freeze(): T  = this //just do nothing, freezing is Kotlin Native only