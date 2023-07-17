package com.valb3r.bpmn.intellij.plugin.bpmn.api

import org.mapstruct.Qualifier

@Qualifier // make sure that this is the MapStruct qualifier annotation
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class DoIgnore