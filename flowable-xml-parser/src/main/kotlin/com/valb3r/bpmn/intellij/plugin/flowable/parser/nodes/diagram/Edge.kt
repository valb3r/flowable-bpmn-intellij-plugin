package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

@Xml
data class Edge (
        @Attribute val id: String?,
        @Attribute val bpmnElement: String?,
        @Element val waypoint: List<Waypoint>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Edge) : EdgeElement
    }
}

@Xml
data class Waypoint(
        @Attribute val x: Float,
        @Attribute val y: Float
)