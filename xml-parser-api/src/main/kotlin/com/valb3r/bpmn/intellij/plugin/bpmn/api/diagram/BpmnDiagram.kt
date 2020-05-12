package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement

@KotlinBuilder
data class DiagramElement(
        val id: DiagramElementId,
        val bpmnPlane: PlaneElement
)