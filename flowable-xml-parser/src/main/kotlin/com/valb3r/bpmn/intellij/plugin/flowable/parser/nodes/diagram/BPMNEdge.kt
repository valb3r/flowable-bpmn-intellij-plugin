package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

data class BPMNEdge (
        @JacksonXmlProperty(isAttribute = true) val id: String?,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String?,
        @JacksonXmlElementWrapper(useWrapping = false) val waypoint: List<Waypoint>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BPMNEdge) : EdgeElement
    }
}

data class Waypoint(
        @JacksonXmlProperty(isAttribute = true) val x: Float,
        @JacksonXmlProperty(isAttribute = true) val y: Float
)