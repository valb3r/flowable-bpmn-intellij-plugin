package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnIntermediateEscalationThrowingEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val incoming: String? = null,
        val outgoing: String? = null,
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}