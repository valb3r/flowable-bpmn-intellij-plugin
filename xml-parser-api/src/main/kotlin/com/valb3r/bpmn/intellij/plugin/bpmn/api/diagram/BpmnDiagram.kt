package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement

data class DiagramElement(
        val id: DiagramElementId,
        val bpmnPlane: PlaneElement
)