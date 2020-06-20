package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder

open class BpmnExtensionElement

@KotlinBuilder
data class ExtensionElement(val name: String?, val string: String?): BpmnExtensionElement()