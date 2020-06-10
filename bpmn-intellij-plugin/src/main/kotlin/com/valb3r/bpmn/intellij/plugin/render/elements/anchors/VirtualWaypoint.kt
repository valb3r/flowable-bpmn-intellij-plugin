package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Point2D

class VirtualWaypoint(
        override val elementId: DiagramElementId,
        private val parentElementId: DiagramElementId,
        private val edge: EdgeWithIdentifiableWaypoints,
        location: Point2D.Float,
        state: RenderState
): CircleAnchorElement(elementId, location, 3.0f, Colors.MID_WAYPOINT_COLOR, state) {

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        if (!isActiveOrDragged()) {
            return mutableListOf()
        }

        return mutableListOf(NewWaypointsEvent(
                parentElementId,
                edge.waypoint
                        .filter { it.physical || it.id == elementId }
                        .map { if (it.id == elementId && !it.physical) it.moveTo(dx, dy).asPhysical() else it }
                        .toList(),
                edge.epoch + 1
        ))
    }
}