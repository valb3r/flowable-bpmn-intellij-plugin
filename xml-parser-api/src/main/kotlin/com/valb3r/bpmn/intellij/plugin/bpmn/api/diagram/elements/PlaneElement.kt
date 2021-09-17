package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

data class PlaneElement(
        override val id: DiagramElementId,
        val bpmnElement: BpmnElementId,
        val bpmnShape: List<ShapeElement>?,
        val bpmnEdge: List<EdgeElement>?
): WithDiagramId