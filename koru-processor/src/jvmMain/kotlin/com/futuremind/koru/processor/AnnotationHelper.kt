package com.futuremind.koru.processor

import com.futuremind.koru.NoScopeProvider
import com.futuremind.koru.ToNativeClass
import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import javax.lang.model.type.MirroredTypeException

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
fun ToNativeClass.launchOnScopeTypeName(): TypeName? {
    //this is the dirtiest hack ever but it works :O
    //there probably is some way of doing this via kotlinpoet-metadata
    //https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
    var scopeProviderTypeName: TypeName? = null
    try {
        this.launchOnScope
    } catch (e: MirroredTypeException) {
        scopeProviderTypeName = e.typeMirror.asTypeName()
    } catch (e: KSTypeNotPresentException){
        scopeProviderTypeName = e.ksType.toTypeName()
    }
    if (scopeProviderTypeName == NoScopeProvider::class.asTypeName()) return null
    return scopeProviderTypeName
}
