package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

data class BpmnOutExtensionElement(
        val source: String,
        val target: String
) : BpmnExtensionElement()