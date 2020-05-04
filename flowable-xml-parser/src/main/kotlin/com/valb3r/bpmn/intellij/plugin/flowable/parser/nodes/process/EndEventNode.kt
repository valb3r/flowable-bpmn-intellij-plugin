package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class EndEventNode(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        @JacksonXmlProperty(isAttribute = true) val documentation: String?
): BpmnMappable<BpmnStartEvent> {

    override fun toElement(): BpmnStartEvent {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper
    interface Mapping {
        fun convertToDto(input: EndEventNode) : BpmnStartEvent
    }
}