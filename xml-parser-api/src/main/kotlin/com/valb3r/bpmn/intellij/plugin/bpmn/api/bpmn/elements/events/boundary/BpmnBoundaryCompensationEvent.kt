package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnBoundaryEventAlike

data class BpmnBoundaryCompensationEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val attachedToRef: BpmnElementId? = null,
        val cancelActivity: Boolean? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
): WithBpmnId, BpmnBoundaryEventAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

