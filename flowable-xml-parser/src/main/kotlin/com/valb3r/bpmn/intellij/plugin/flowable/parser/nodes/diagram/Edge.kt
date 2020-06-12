package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElementWrapper

class Edge {

    @XmlAttribute val id: String? = null
    @XmlAttribute val bpmnElement: String? = null
    @XmlElementWrapper val waypoint: List<Waypoint>? = null

    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Edge) : EdgeElement
    }
}

class Waypoint {
    @XmlAttribute val x: Float = 0.0f
    @XmlAttribute val y: Float = 0.0f
}