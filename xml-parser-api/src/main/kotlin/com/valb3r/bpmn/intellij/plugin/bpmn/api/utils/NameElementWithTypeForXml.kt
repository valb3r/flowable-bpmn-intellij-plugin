package com.valb3r.bpmn.intellij.plugin.bpmn.api.utils

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import kotlin.reflect.KClass

data class NameElementWithTypeForXml<T : WithBpmnId>(val clazz: KClass<T>, val type: String)