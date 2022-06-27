package com.futuremind.koru.processor

import com.futuremind.koru.NoScopeProvider
import com.futuremind.koru.ToNativeClass
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.type.MirroredTypeException

//TODO
fun ToNativeClass.kaptLaunchOnScopeTypeName(): TypeName? {
    //this is the dirtiest hack ever but it works :O
    //https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
    var scopeProviderTypeName: TypeName? = null
    try {
        this.launchOnScope
    } catch (e: MirroredTypeException) {
        scopeProviderTypeName = e.typeMirror.asTypeName()
    }
    if (scopeProviderTypeName == NoScopeProvider::class.asTypeName()) return null
    return scopeProviderTypeName
}

