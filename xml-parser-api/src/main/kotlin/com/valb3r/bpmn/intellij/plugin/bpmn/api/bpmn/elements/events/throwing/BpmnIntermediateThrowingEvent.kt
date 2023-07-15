package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnEscalationEventDefinition

data class BpmnIntermediateThrowingEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
        val signalEventDefinition: SignalEventDefinition? = null,
        val escalationEventDefinition: BpmnEscalationEventDefinition? = null
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class SignalEventDefinition(
            val signalRef: String? = null
    )
}