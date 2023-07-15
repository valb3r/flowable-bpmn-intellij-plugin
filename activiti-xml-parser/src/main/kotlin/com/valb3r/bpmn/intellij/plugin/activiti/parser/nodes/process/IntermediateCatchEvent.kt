package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.TimerEventDefinition
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateCatchingEvent
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class IntermediateCatchEvent(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val timerEventDefinition: TimerEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val signalEventDefinition: SignalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val messageEventDefinition: MessageEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val conditionalEventDefinition: ConditionalEventDefinition?

): BpmnMappable<BpmnIntermediateCatchingEvent> {

    override fun toElement(): BpmnIntermediateCatchingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: IntermediateCatchEvent) : BpmnIntermediateCatchingEvent
    }

    data class SignalEventDefinition(
            @JacksonXmlProperty(isAttribute = true) val signalRef: String? = null
    )
}