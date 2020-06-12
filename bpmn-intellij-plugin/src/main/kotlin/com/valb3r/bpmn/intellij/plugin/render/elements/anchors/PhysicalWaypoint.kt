package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ResizeViewTransform
import java.awt.geom.Point2D
import java.util.*

class PhysicalWaypoint(
        override val elementId: DiagramElementId,
        private val parentElementId: DiagramElementId,
        private val parentElementBpmnId: BpmnElementId?,
        private val edge: EdgeWithIdentifiableWaypoints,
        private val physicalPos: Int,
        private val edgePhysicalSize: Int,
        location: Point2D.Float,
        state: RenderState
): CircleAnchorElement(elementId, location, 3.0f, Colors.WAYPOINT_COLOR, state) {

    override fun drawActions(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        if (isEdgeBeginOrEnd()) {
            return mutableMapOf()
        }

        val delId = DiagramElementId("DEL:$elementId")
        val deleteIconArea = state.ctx.canvas.drawIcon(BoundsElement(x, y, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state.icons.recycleBin)
        state.ctx.interactionContext.clickCallbacks[delId] = { dest ->
            dest.addEvents(listOf(NewWaypointsEvent(
                    parentElementId,
                    edge.waypoint
                            .filter { it.physical }
                            .filter { it.id != elementId }
                            .toList(),
                    edge.epoch + 1
            )))
        }
        return mutableMapOf(delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(),  ICON_Z_INDEX, elementId))
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOn: SortedMap<AreaType, BpmnElementId>): MutableList<Event> {
        val events = mutableListOf<Event>()

        events += DraggedToEvent(elementId, dx, dy, parentElementId, physicalPos) as Event

        if (null == parentElementBpmnId) {
            return events
        }

        if (null != droppedOn && !multipleElementsSelected()) {
            if (edgePhysicalSize - 1 == physicalPos) {
                events += StringValueUpdatedEvent(parentElementBpmnId, PropertyType.TARGET_REF, droppedOn.id)
            } else if (0 == physicalPos) {
                events += StringValueUpdatedEvent(parentElementBpmnId, PropertyType.SOURCE_REF, droppedOn.id)
            }
        }

        return events
    }

    override fun doComputeLocationChangesBasedOnTransformationWithCascade(): MutableList<Event> {
        val transform = viewTransform
        if (transform !is ResizeViewTransform) {
            return mutableListOf()
        }

        return mutableListOf(
                DraggedToEvent(
                        elementId,
                        transformedLocation.x - currentLocation.x,
                        transformedLocation.y - currentLocation.y,
                        parentElementId,
                        physicalPos
                )
        )
    }

    override fun ifVisibleNoRenderIf(): Boolean {
        return !isRenderable()
    }

    override fun isRenderable(): Boolean {
        return if (isEdgeBeginOrEnd()) {
            return !multipleElementsSelected()
        } else {
            true
        }
    }

    private fun isEdgeBeginOrEnd() = edgePhysicalSize - 1 == physicalPos || 0 == physicalPos
}