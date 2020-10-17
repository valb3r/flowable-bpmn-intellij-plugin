package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.BpmnElementIdMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import org.mapstruct.Mapper

data class Edge (
        @JacksonXmlProperty(isAttribute = true) val id: String?,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String?,
        @JacksonXmlElementWrapper(useWrapping = false) val waypoint: List<Waypoint>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Edge) : EdgeElement
    }
}

data class Waypoint(
        @JacksonXmlProperty(isAttribute = true) val x: Float,
        @JacksonXmlProperty(isAttribute = true) val y: Float
)