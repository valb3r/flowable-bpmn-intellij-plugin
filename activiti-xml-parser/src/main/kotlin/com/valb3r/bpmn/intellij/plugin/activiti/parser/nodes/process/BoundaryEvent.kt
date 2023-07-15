package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.TimerEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryEvent
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class BoundaryEvent(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val attachedToRef: String?,
    @JacksonXmlProperty(isAttribute = true) val cancelActivity: Boolean?,
    val documentation: String?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val timerEventDefinition: TimerEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val signalEventDefinition: SignalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val messageEventDefinition: MessageEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val errorEventDefinition: ErrorEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val cancelEventDefinition: CancelEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val compensateEventDefinition: CompensateEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val conditionalEventDefinition: ConditionalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val escalationEventDefinition: EscalationEventDefinition?
): BpmnMappable<BpmnBoundaryEvent> {

    override fun toElement(): BpmnBoundaryEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BoundaryEvent) : BpmnBoundaryEvent
    }

    data class SignalEventDefinition(
            val signalRef: String? = null
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

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )
}