package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

open class BpmnExtensionElement

data class ExtensionElement(val name: String? = null, val string: String? = null, val expression: String?): BpmnExtensionElement()

data class ExtensionField(val name: String? = null, val string: String? = null, val expression: String?)

data class ExtensionFormProperty(
    val id: String?, val name: String?, val type: String?, val expression: String?, val variable: String?, val default: String?,
    val datePattern: String?, val value: List<ExtensionFormPropertyValue>?
)

data class ExtensionFormPropertyValue(val id: String?, val name: String?)

data class UnmappedProperty(val name: String? = null, val string: String? = null)

