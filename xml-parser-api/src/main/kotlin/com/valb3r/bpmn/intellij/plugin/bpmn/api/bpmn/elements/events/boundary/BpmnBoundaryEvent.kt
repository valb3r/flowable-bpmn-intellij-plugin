package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnBoundaryEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val attachedToRef: BpmnElementId? = null,
        val cancelActivity: Boolean? = null,
        val documentation: String? = null,
        val timerEventDefinition: TimerEventDefinition? = null,
        val signalEventDefinition: SignalEventDefinition? = null,
        val messageEventDefinition: MessageEventDefinition? = null,
        val errorEventDefinition: ErrorEventDefinition? = null,
        val cancelEventDefinition: CancelEventDefinition? = null,
        val escalationEventDefinition: EscalationEventDefinition? = null,
        val conditionalEventDefinition: ConditionalEventDefinition? = null,
        val compensateEventDefinition: CompensateEventDefinition? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class TimerEventDefinition(
        val timeDate: String?,
        val timeDuration: String?,
        val timeCycle: String?,
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
            val cancelRef: String? = null // TODO - what it cancels? = null
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