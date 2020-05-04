package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnMappable
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class BPMNEdge (
        @JacksonXmlProperty(isAttribute = true) val id: String?,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String?,
        @JacksonXmlElementWrapper(useWrapping = false) val waypoint: List<Waypoint>?
): BpmnMappable<BpmnCallActivity> {

    override fun toElement(): BpmnCallActivity {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper
    interface Mapping {
        fun convertToDto(input: BPMNEdge) : BpmnCallActivity
    }
}

data class Waypoint(
        @JacksonXmlProperty(isAttribute = true) val x: Float,
        @JacksonXmlProperty(isAttribute = true) val y: Float
)