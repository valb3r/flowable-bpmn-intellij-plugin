package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnIntermediateThrowingEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val incoming: String? = null,
        val outgoing: String? = null,
        val signalEventDefinition: SignalEventDefinition? = null,
        val escalationEventDefinition: EscalationEventDefinition? = null
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class SignalEventDefinition(
            val signalRef: String? = null
    )

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )
}