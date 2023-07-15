package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateCatchingEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.MessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.SignalEventDefinition
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.TimerEventDefinition
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class IntermediateCatchEvent(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val timerEventDefinition: TimerEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val signalEventDefinition: SignalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val messageEventDefinition: MessageEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val conditionalEventDefinition: ConditionalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val linkEventDefinition: LinkEventDefinition?
): BpmnMappable<BpmnIntermediateCatchingEvent> {

    override fun toElement(): BpmnIntermediateCatchingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class, LinkEventDefinitionMapping::class])
    interface Mapping {
        fun convertToDto(input: IntermediateCatchEvent) : BpmnIntermediateCatchingEvent
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface LinkEventDefinitionMapping {
        fun convertToDto(input: LinkEventDefinition) : BpmnIntermediateCatchingEvent.LinkEventDefinition
    }

    data class LinkEventDefinition(
        @JacksonXmlProperty(isAttribute = true) val id: String? = null,
        @JacksonXmlProperty(isAttribute = true) val name: String? = null
    )
}