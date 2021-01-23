package com.futuremind.iossuspendwrapper

import kotlin.native.concurrent.freeze

actual fun <T> T.freeze(): T = this.freeze()