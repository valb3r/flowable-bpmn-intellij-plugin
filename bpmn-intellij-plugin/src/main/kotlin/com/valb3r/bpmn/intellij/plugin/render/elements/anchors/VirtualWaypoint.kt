package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D

class VirtualWaypoint(
        override val elementId: DiagramElementId,
        location: Point2D.Float,
        state: CurrentState
): CircleAnchorElement(elementId, location, 3.0f, Colors.MID_WAYPOINT_COLOR, state)