package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import java.time.LocalDateTime

data class BpmnIntermediateCatchingEvent(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val timerEventDefinition: TimerEventDefinition?,
        val signalEventDefinition: SignalEventDefinition?,
        val messageEventDefinition: MessageEventDefinition?,
        val conditionalEventDefinition: ConditionalEventDefinition?
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class TimerEventDefinition(
        val timeDate: LocalDateTime? = null
)

data class SignalEventDefinition(
        val signalRef: String? = null
)

data class MessageEventDefinition(
        val messageRef: String? = null
)

data class ConditionalEventDefinition(
        val condition: String? = null
)