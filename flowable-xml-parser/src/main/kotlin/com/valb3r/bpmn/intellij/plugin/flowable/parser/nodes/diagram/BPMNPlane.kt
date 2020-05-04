package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class BPMNPlane(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String,
        @JacksonXmlProperty(localName = "BPMNShape") @JacksonXmlElementWrapper(useWrapping = false) val bpmnShape: List<BPMNShape>?,
        @JacksonXmlProperty(localName = "BPMNEdge") @JacksonXmlElementWrapper(useWrapping = false) val bpmnEdge: List<BPMNEdge>?
)