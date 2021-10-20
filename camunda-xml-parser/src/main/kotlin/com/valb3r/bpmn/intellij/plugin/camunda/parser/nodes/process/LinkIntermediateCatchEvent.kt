package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnLinkIntermediateCatchingEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class LinkIntermediateCatchEvent(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val linkEventDefinition: LinkEventDefinition?,
): BpmnMappable<BpmnLinkIntermediateCatchingEvent> {

    override fun toElement(): BpmnLinkIntermediateCatchingEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class, LinkEventDefinitionMapping::class])
    interface Mapping {
        fun convertToDto(input: LinkIntermediateCatchEvent) : BpmnLinkIntermediateCatchingEvent
    }

    @Mapper
    interface LinkEventDefinitionMapping {
        fun map(definition: LinkEventDefinition): com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.LinkEventDefinition
    }

    data class LinkEventDefinition(
        val id: String? = null,
        val condition: String? = null
    )
}