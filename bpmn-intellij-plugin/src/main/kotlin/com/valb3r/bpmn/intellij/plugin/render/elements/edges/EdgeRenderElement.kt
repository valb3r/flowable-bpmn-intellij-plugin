package com.valb3r.bpmn.intellij.plugin.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState

class EdgeRenderElement(
        override val elementId: DiagramElementId,
        edge: EdgeWithIdentifiableWaypoints,
        state: RenderState
): BaseEdgeRenderElement(elementId, edge, Colors.ARROW_COLOR, state)