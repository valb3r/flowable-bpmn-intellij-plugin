package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import java.time.LocalDateTime

data class BpmnIntermediateCatchingEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val timerEventDefinition: TimerEventDefinition? = null,
        val signalEventDefinition: SignalEventDefinition? = null,
        val messageEventDefinition: MessageEventDefinition? = null,
        val conditionalEventDefinition: ConditionalEventDefinition? = null,
        val linkEventDefinition: LinkEventDefinition? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
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

data class ConditionalEventDefinition(
        val condition: String? = null
)

data class LinkEventDefinition(
        val id: String? = null,
        val condition: String? = null
)