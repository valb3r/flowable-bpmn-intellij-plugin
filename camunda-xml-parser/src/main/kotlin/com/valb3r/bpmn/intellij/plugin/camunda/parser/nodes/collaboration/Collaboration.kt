package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.collaboration

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnCollaboration
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class Collaboration(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val participant: List<Participant>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val messageFlow: List<MessageFlow>?,
) : BpmnMappable<BpmnCollaboration> {

    override fun toElement(): BpmnCollaboration {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Collaboration): BpmnCollaboration
    }
}

data class Participant(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val processRef: String?,
    val documentation: String?
)

data class MessageFlow(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    val documentation: String?,
    @JacksonXmlProperty(isAttribute = true) val sourceRef: String?,
    @JacksonXmlProperty(isAttribute = true) val targetRef: String?,
)

