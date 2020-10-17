package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.geom.Point2D

private const val RADIUS = 3.0f

class VirtualWaypoint(
        elementId: DiagramElementId,
        private val parentElementId: DiagramElementId,
        private val edge: EdgeWithIdentifiableWaypoints,
        location: Point2D.Float,
        state: RenderState
): CircleAnchorElement(elementId, null, location, RADIUS, Colors.MID_WAYPOINT_COLOR, state) {

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        if (!isActive() || !isDragged()) {
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

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return if (isActiveOrDragged()) return super.waypointAnchors(camera) else mutableSetOf()
    }
}