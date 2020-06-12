package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElementWrapper

@XmlAccessorType(XmlAccessType.FIELD)
class Plane {

    @XmlAttribute val id: String = ""
    @XmlAttribute val bpmnElement: String = ""
    // variable name sensitive
    @XmlElementWrapper(name = "BPMNShape") val bpmnShape: List<Shape>? = null
    // variable name sensitive
    @XmlElementWrapper(name = "BPMNEdge") val bpmnEdge: List<Edge>? = null

    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Plane) : PlaneElement
    }
}