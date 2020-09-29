package com.valb3r.bpmn.intellij.plugin.activity.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.activity.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

data class Shape(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String,
        val bounds: Bounds
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Shape) : ShapeElement
    }
}

data class Bounds(
        @JacksonXmlProperty(isAttribute = true) val x: Float,
        @JacksonXmlProperty(isAttribute = true) val y: Float,
        @JacksonXmlProperty(isAttribute = true) val width: Float,
        @JacksonXmlProperty(isAttribute = true) val height: Float
)