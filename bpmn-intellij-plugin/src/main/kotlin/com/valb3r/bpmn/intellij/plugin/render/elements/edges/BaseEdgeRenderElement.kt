package com.valb3r.bpmn.intellij.plugin.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.AnchorElement
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.PhysicalWaypoint
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.VirtualWaypoint
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class BaseEdgeRenderElement(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        protected val edge: EdgeWithIdentifiableWaypoints,
        private val edgeColor: Colors,
        state: RenderState
): BaseBpmnRenderElement(elementId, bpmnElementId, state) {

    private val anchors = computeAnchors()

    override val children: MutableList<BaseDiagramRenderElement> = anchors as MutableList<BaseDiagramRenderElement>

    override fun dragTo(dx: Float, dy: Float) {
        if (multipleElementsSelected() && isActiveOrDragged()) {
            return
        }

        super.dragTo(dx, dy)
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val area = Area()

        val activeWaypoints = anchors.filter { it is PhysicalWaypoint || (!multipleElementsSelected() && it.isActiveOrDragged()) }
        val updatedAnchors = activeWaypoints.map { it.transformedLocation }

        updatedAnchors.forEachIndexed {pos, waypoint ->
            when {
                pos == updatedAnchors.size - 1 -> area.add(ctx.canvas.drawLineWithArrow(updatedAnchors[pos - 1], waypoint, color(isActiveEdge(pos, activeWaypoints), edgeColor)))
                pos > 0 -> area.add(ctx.canvas.drawLine(updatedAnchors[pos - 1], waypoint, color(isActiveEdge(pos, activeWaypoints), edgeColor)))
            }
        }

        if (state.history.contains(bpmnElementId)) {
            val indexes = state.history.mapIndexed {pos, id -> if (id == bpmnElementId) pos else null}.filterNotNull()
            val midPoints = anchors.filterIsInstance<VirtualWaypoint>().map { it.transformedLocation }
            state.ctx.canvas.drawTextNoCameraTransform(midPoints[midPoints.size / 2], indexes.toString(), Colors.INNER_TEXT_COLOR.color, Colors.DEBUG_ELEMENT_COLOR.color)
        }
        area.add(renderDefaultMarkIfNeeded(ctx, anchors.filterIsInstance<PhysicalWaypoint>().map { it.transformedLocation }))
        return mapOf(elementId to AreaWithZindex(area, AreaType.EDGE, waypointAnchors(ctx.canvas.camera), index = zIndex()))
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        return mutableListOf()
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return edge.waypoint.filter { it.physical && !state.ctx.selectedIds.contains(it.id) }.map { Anchor(Point2D.Float(it.x, it.y)) }.toMutableSet()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val minX = edge.waypoint.minBy { it.x }?.x ?: 0.0f
        val minY = edge.waypoint.minBy { it.y }?.y ?: 0.0f
        val maxX = edge.waypoint.maxBy { it.x }?.x ?: 0.0f
        val maxY = edge.waypoint.maxBy { it.y }?.y ?: 0.0f

        // Edge itself can't be translated, so no viewTransform
        return Rectangle2D.Float(
                minX,
                minY,
                maxX - minX,
                maxY - minY
        )
    }

    private fun renderDefaultMarkIfNeeded(ctx: RenderContext, anchors: List<Point2D.Float>): Area {
        val sourceRefOf = state.currentState.elemPropertiesByStaticElementId.filter { it.value[PropertyType.DEFAULT_FLOW]?.value == bpmnElementId.id }
        if (sourceRefOf.isEmpty()) {
            return Area()
        }

        return ctx.canvas.drawLineSlash(anchors[0], anchors[1], color(edgeColor))
    }

    private fun isActiveEdge(endPos: Int, activeAnchors: List<AnchorElement>): Boolean {
        if (isActive()) {
            return true
        }
        val leftAnchorActive = activeAnchors[endPos - 1].isActive()
        val rightAnchorActive = activeAnchors[endPos].isActive()
        val anyAnchorActive = leftAnchorActive || rightAnchorActive

        return (leftAnchorActive && rightAnchorActive) || (anyAnchorActive && endPos == activeAnchors.size - 1) || (anyAnchorActive && endPos - 1 == 0)
    }

    private fun computeAnchors(): List<AnchorElement> {
        val numPhysicals = edge.waypoint.filter { it.physical }.size
        var physicalPos = -1
        return edge.waypoint.map {
            if (it.physical) {
                physicalPos++
                PhysicalWaypoint(it.id, edge.id, edge.bpmnElement, edge, physicalPos, numPhysicals, Point2D.Float(it.x, it.y), state).let { it.parents.add(this); it }
            } else {
                VirtualWaypoint(it.id, edge.id, edge, Point2D.Float(it.x, it.y), state).let { it.parents.add(this); it }
            }
        }
    }
}