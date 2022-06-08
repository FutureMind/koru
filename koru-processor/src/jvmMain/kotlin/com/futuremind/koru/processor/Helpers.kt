package com.futuremind.koru.processor

fun String.nonEmptyOr(or: String) = this.ifEmpty { or }