package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class EventBasedGateway(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        @JacksonXmlProperty(isAttribute = true, localName = "default") val defaultElement: String?,
        val documentation: String?
): BpmnMappable<BpmnEventGateway> {

    override fun toElement(): BpmnEventGateway {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: EventBasedGateway) : BpmnEventGateway
    }
}