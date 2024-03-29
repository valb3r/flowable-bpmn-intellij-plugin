package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import java.awt.geom.Rectangle2D

data class ShapeCtx(
        val diagramId: DiagramElementId,
        val bpmnId: BpmnElementId?,
        val shape: Rectangle2D.Float,
        val properties: PropertyTable?,
        val name: String?
)