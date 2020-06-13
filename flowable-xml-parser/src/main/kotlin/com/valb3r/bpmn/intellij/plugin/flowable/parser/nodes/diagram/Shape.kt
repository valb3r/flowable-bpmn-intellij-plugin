package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.Xml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

@Xml
data class Shape(
        @Attribute val id: String,
        @Attribute val bpmnElement: String,
        @Element val bounds: Bounds
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Shape) : ShapeElement
    }
}

@Xml
data class Bounds(
        @Attribute val x: Float,
        @Attribute val y: Float,
        @Attribute val width: Float,
        @Attribute val height: Float
)