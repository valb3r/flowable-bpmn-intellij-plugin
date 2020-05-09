package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper

data class BPMNPlane(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String,
        @JacksonXmlProperty(localName = "BPMNShape") @JacksonXmlElementWrapper(useWrapping = false) val bpmnShape: List<BPMNShape>?,
        @JacksonXmlProperty(localName = "BPMNEdge") @JacksonXmlElementWrapper(useWrapping = false) val bpmnEdge: List<BPMNEdge>?
) {
    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: BPMNPlane) : PlaneElement
    }
}