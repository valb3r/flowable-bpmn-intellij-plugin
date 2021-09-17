package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

open class BpmnExtensionElement

data class ExtensionElement(val name: String?, val string: String?, val expression: String?): BpmnExtensionElement()