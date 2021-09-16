package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnCallActivity(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val calledElement: String?,
        val calledElementType: String?,
        val inheritVariables: Boolean?,
        val fallbackToDefaultTenant: Boolean?,
        val extensionElements: ExtensionElements?
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class ExtensionElements(
        val out: List<OutExtensionElement>?
)

data class OutExtensionElement(val source: String?, val target: String?)