package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Point2D

class PhysicalWaypoint(
        override val elementId: DiagramElementId,
        location: Point2D.Float,
        state: RenderState
): CircleAnchorElement(elementId, location, 3.0f, Colors.WAYPOINT_COLOR, state) {

    override fun needsParentActiveToAcceptEvents(): Boolean {
        return false
    }
}