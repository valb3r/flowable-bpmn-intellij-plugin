package com.valb3r.bpmn.intellij.plugin.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.AnchorElement
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.PhysicalWaypoint
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.VirtualWaypoint
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class BaseEdgeRenderElement(
        override val elementId: DiagramElementId,
        protected val edge: EdgeWithIdentifiableWaypoints,
        private val edgeColor: Colors,
        state: RenderState
): BaseRenderElement(elementId, state) {

    private val anchors = computeAnchors()

    override val children: MutableList<BaseRenderElement> = anchors as MutableList<BaseRenderElement>

    override fun dragTo(dx: Float, dy: Float) {
        // No drag support for edge itself
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val area = Area()

        val updatedAnchors = anchors.filter { it is PhysicalWaypoint || (!multipleElementsSelected() && it.isActiveOrDragged()) }.map { it.transformedLocation }

        updatedAnchors.forEachIndexed {pos, waypoint ->
            when {
                pos == updatedAnchors.size - 1 -> area.add(ctx.canvas.drawLineWithArrow(updatedAnchors[pos - 1], waypoint, color(isActive(), edgeColor)))
                pos > 0 -> area.add(ctx.canvas.drawLine(updatedAnchors[pos - 1], waypoint, color(isActive(), edgeColor)))
            }
        }

        return mapOf(elementId to AreaWithZindex(area, AreaType.EDGE, waypointAnchors(ctx.canvas.camera)))
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        return mutableListOf()
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return edge.waypoint.filter { it.physical && !state.ctx.selectedIds.contains(it.id) }.map { Point2D.Float(it.x, it.y) }.toMutableSet()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun currentRect(camera: Camera): Rectangle2D.Float {
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

    private fun computeAnchors(): List<AnchorElement> {
        val numPhysicals = edge.waypoint.filter { it.physical }.size
        var physicalPos = -1
        return edge.waypoint.map {
            if (it.physical) {
                physicalPos++
                PhysicalWaypoint(it.id, edge.id, edge.bpmnElement, edge, physicalPos, numPhysicals, Point2D.Float(it.x, it.y), state)
            } else {
                VirtualWaypoint(it.id, edge.id, edge, Point2D.Float(it.x, it.y), state)
            }
        }
    }
}