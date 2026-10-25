package com.valb3r.bpmn.intellij.plugin.core.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.core.render.MIN_ORTHOGONAL_WAYPOINTS
import com.valb3r.bpmn.intellij.plugin.core.render.orthogonalWaypoints
import com.valb3r.bpmn.intellij.plugin.core.render.elements.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.orthoIconIdPrefix
import java.awt.geom.Point2D

class EdgeRenderElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        edge: EdgeWithIdentifiableWaypoints,
        state: () -> RenderState,
        arrowAtEnd: Boolean = true,
): BaseEdgeRenderElement(elementId, bpmnElementId, edge, Colors.ARROW_COLOR, state, arrowAtEnd) {

    override val areaType: AreaType
        get() =  AreaType.EDGE

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val actions = mutableMapOf(drawDeleteAction(x, y))
        if (canMakeOrthogonal()) {
            actions += drawOrthogonalAction(x, y)
        }

        return actions
    }

    private fun drawDeleteAction(x: Float, y: Float): Pair<DiagramElementId, AreaWithZindex> {
        val delId = elementId.elemIdToRemove()
        val deleteIconArea = state().ctx.canvas.drawIcon(BoundsElement(x, y - ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state().icons.recycleBin)
        state().ctx.interactionContext.clickCallbacks[delId] = { dest ->
            val currentProps = state().currentState.propertyWithElementByPropertyType
            val cascadeEvents = computeCascadeChangeOfBpmnIncomingOutgoingIndex(bpmnElementId, currentProps, PropertyType.BPMN_INCOMING).toMutableList() +
                    computeCascadeChangeOfBpmnIncomingOutgoingIndex(bpmnElementId, currentProps, PropertyType.BPMN_OUTGOING)
            dest.addElementRemovedEvent(getEventsToDeleteDiagram(), getEventsToDeleteElement(), cascadeEvents)
        }

        return delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
    }

    private fun canMakeOrthogonal(): Boolean {
        val bpmnElement = state().currentState.elementByBpmnId[bpmnElementId]?.element
        if (bpmnElement !is BpmnSequenceFlow) return false
        val points = edge.waypoint.filter { it.physical }
        val hasDiagonal = points.zipWithNext().any { (start, end) -> start.x != end.x && start.y != end.y }

        return points.size < MIN_ORTHOGONAL_WAYPOINTS || hasDiagonal
    }

    private fun drawOrthogonalAction(x: Float, y: Float): Pair<DiagramElementId, AreaWithZindex> {
        val camera = state().ctx.canvas.camera
        val deleteTop = camera.toCameraView(Point2D.Float(x, y - ACTIONS_ICO_SIZE))
        val nextTop = camera.fromCameraView(Point2D.Float(deleteTop.x, deleteTop.y + ACTIONS_ICO_SIZE))
        val orthoId = DiagramElementId(orthoIconIdPrefix + elementId.id)
        val area = state().ctx.canvas.drawIcon(BoundsElement(nextTop.x, nextTop.y, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state().icons.rightAngle)
        state().ctx.interactionContext.clickCallbacks[orthoId] = { dest ->
            dest.addEvents(listOf(NewWaypointsEvent(elementId, edge.orthogonalWaypoints(), edge.epoch + 1)))
        }
        return orthoId to AreaWithZindex(area, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
    }
}
