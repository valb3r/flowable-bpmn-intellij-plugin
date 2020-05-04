package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class BPMNShape(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val bpmnElement: String,
        val bounds: Bounds
)

data class Bounds(
        @JacksonXmlProperty(isAttribute = true) val x: Float,
        @JacksonXmlProperty(isAttribute = true) val y: Float,
        @JacksonXmlProperty(isAttribute = true) val width: Float,
        @JacksonXmlProperty(isAttribute = true) val height: Float
)