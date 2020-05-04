package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder

@KotlinBuilder
data class PlaneElement(
        val id: String,
        val bpmnElement: String,
        val bpmnShape: List<ShapeElement>?,
        val bpmnEdge: List<EdgeElement>?
)