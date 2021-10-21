package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnCallActivity(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val calledElement: String? = null,
        val calledElementType: String? = null,
        val inheritVariables: Boolean? = null,
        val fallbackToDefaultTenant: Boolean? = null,
        val extensionElements: ExtensionElements? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class ExtensionElements(
        val out: List<OutExtensionElement>? = null
)

data class OutExtensionElement(val source: String? = null, val target: String?)