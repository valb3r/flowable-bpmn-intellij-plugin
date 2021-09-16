package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnBoundaryEvent(
        override val id: BpmnElementId,
        val name: String?,
        val attachedToRef: BpmnElementId?,
        val cancelActivity: Boolean?,
        val documentation: String?,
        val timerEventDefinition: TimerEventDefinition?,
        val signalEventDefinition: SignalEventDefinition?,
        val messageEventDefinition: MessageEventDefinition?,
        val errorEventDefinition: ErrorEventDefinition?,
        val cancelEventDefinition: CancelEventDefinition?,
        val escalationEventDefinition: EscalationEventDefinition?,
        val conditionalEventDefinition: ConditionalEventDefinition?,
        val compensateEventDefinition: CompensateEventDefinition?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class TimerEventDefinition(
            val timeDate: String? = null
    )

    data class SignalEventDefinition(
            val signalRef: String? = null
    )

    data class MessageEventDefinition(
            val messageRef: String? = null
    )

    data class ErrorEventDefinition(
            val errorRef: String? = null
    )

    data class CancelEventDefinition(
            val cancelRef: String? = null // TODO - what it cancels?
    )

    data class CompensateEventDefinition(
            val activityRef: String? = null
    )

    data class ConditionalEventDefinition(
            val condition: String? = null
    )

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )
}