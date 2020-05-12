package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

interface WithDiagramId {
    val id: DiagramElementId
}