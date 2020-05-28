package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

// For mixed lists in XML we need to have JsonSetter/JsonMerge on field
// https://github.com/FasterXML/jackson-dataformat-xml/issues/363
// unfortunately this has failed with Kotlin 'data' classes
data class Plane(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String,
        // variable name sensitive
        @JsonMerge @JacksonXmlProperty(localName = "BPMNShape") @JacksonXmlElementWrapper(useWrapping = false) val bpmnShape: List<Shape>?,
        // variable name sensitive
        @JsonMerge @JacksonXmlProperty(localName = "BPMNEdge") @JacksonXmlElementWrapper(useWrapping = false) val bpmnEdge: List<Edge>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Plane) : PlaneElement
    }
}