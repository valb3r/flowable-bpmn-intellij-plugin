package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnBoundaryConditionalEvent(
        override val id: BpmnElementId,
        val name: String?,
        val attachedToRef: BpmnElementId?,
        val cancelActivity: Boolean?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

