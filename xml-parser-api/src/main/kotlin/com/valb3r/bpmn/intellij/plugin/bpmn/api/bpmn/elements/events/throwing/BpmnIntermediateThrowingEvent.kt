package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

@KotlinBuilder
data class BpmnIntermediateThrowingEvent(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val signalEventDefinition: SignalEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?
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