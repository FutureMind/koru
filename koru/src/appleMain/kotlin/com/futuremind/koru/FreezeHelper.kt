package com.futuremind.koru

import kotlin.native.concurrent.freeze

actual fun <T> T.freeze(): T = this.freeze()