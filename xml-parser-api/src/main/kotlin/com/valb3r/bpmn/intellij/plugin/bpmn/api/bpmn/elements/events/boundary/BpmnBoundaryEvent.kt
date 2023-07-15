package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnEscalationEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnMessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props.BpmnTimerEventDefinition

data class BpmnBoundaryEvent(
    override val id: BpmnElementId,
    val name: String? = null,
    val attachedToRef: BpmnElementId? = null,
    val cancelActivity: Boolean? = null,
    val documentation: String? = null,
    val timerEventDefinition: BpmnTimerEventDefinition? = null,
    val signalEventDefinition: SignalEventDefinition? = null,
    val messageEventDefinition: BpmnMessageEventDefinition? = null,
    val errorEventDefinition: ErrorEventDefinition? = null,
    val cancelEventDefinition: CancelEventDefinition? = null,
    val escalationEventDefinition: BpmnEscalationEventDefinition? = null,
    val conditionalEventDefinition: BpmnConditionalEventDefinition? = null,
    val compensateEventDefinition: CompensateEventDefinition? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }

    data class SignalEventDefinition(
            val signalRef: String? = null
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
}