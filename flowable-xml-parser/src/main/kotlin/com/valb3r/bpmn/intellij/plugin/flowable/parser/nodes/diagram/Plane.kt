package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

@Xml
data class Plane(
        @Attribute val id: String,
        @Attribute val bpmnElement: String,
        // variable name sensitive
        @Element(name = "bpmndi:BPMNShape") val bpmnShape: List<Shape>?,
        // variable name sensitive
        @Element(name = "bpmndi:BPMNEdge") val bpmnEdge: List<Edge>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Plane) : PlaneElement
    }
}