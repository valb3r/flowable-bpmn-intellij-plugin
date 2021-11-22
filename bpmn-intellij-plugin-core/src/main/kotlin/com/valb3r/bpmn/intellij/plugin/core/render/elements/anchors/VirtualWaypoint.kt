package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.edges.BaseEdgeRenderElement
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

private const val RADIUS = 3.0f

class VirtualWaypoint(
        elementId: DiagramElementId,
        private val parentElementId: DiagramElementId,
        private val edge: EdgeWithIdentifiableWaypoints,
        location: Point2D.Float,
        state: () -> RenderState
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

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val result = mutableMapOf<DiagramElementId, AreaWithZindex>()
        val targetBounds = BoundsElement(x, y - ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE)
        addEdgeSelectionButton(targetBounds, result)
        return result
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return if (isActiveOrDragged()) return super.waypointAnchors(camera) else mutableSetOf()
    }

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val edge = state().elemMap[parentElementId] as BaseEdgeRenderElement
        val edgeElems = edge.children.filter { it is PhysicalWaypoint || it.elementId == elementId }
        val indexOfCurrentPoint = edgeElems.indexOfFirst { it.elementId == elementId }
        val prevRect = edgeElems[indexOfCurrentPoint - 1].currentOnScreenRect(camera)
        val nextRect = edgeElems[indexOfCurrentPoint + 1].currentOnScreenRect(camera)
        val currentPointLocation = Point2D.Float((prevRect.centerX.toFloat() + nextRect.centerX.toFloat()) / 2.0f, (prevRect.centerY.toFloat() + nextRect.centerY.toFloat()) / 2.0f)
        return Rectangle2D.Float(
            currentPointLocation.x - radius,
            currentPointLocation.y - radius,
            2.0f * radius,
            2.0f * radius
        )
    }
}
