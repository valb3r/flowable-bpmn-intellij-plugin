package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class ParallelGateway(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        @JacksonXmlProperty(isAttribute = true, localName = "default") val defaultElement: String?,
        val incoming: String?,
        val outgoing: String?,
        val documentation: String?
): BpmnMappable<BpmnParallelGateway> {

    override fun toElement(): BpmnParallelGateway {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ParallelGateway) : BpmnParallelGateway
    }
}