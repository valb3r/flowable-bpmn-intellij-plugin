package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.BpmnElementIdMapper
import org.mapstruct.Mapper
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
class Shape {

    @XmlAttribute val id: String? = ""
    @XmlAttribute val bpmnElement? = ""
    val bounds: Bounds? = null

    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: Shape) : ShapeElement
    }
}

@XmlAccessorType(XmlAccessType.FIELD)
class Bounds {

    @XmlAttribute val x: Float? = null
    @XmlAttribute val y: Float? = null
    @XmlAttribute val width: Float? = null
    @XmlAttribute val height: Float? = null
}