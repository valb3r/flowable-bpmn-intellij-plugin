package com.valb3r.bpmn.intellij.plugin.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.PhysicalWaypoint
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.VirtualWaypoint
import java.awt.Event
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class BaseEdgeRenderElement(
        override val elementId: DiagramElementId,
        private val edge: EdgeWithIdentifiableWaypoints,
        private val edgeColor: Colors,
        state: RenderState
): BaseRenderElement(elementId, state) {

    private val anchors = edge.waypoint.map {
        if (it.physical) {
            PhysicalWaypoint(it.id, Point2D.Float(it.x, it.y), state) as BaseRenderElement
        } else {
            VirtualWaypoint(it.id, Point2D.Float(it.x, it.y), state) as BaseRenderElement
        }
    }.toMutableList()

    override val children: MutableList<BaseRenderElement> = anchors

    override fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val area = Area()

        edge.waypoint.forEachIndexed {pos, waypoint ->
            when {
                pos == edge.waypoint.size - 1 -> area.add(ctx.canvas.drawLineWithArrow(edge.waypoint[pos - 1].asWaypointElement(), waypoint.asWaypointElement(), color(isActive(), edgeColor)))
                pos > 0 -> area.add(ctx.canvas.drawLine(edge.waypoint[pos - 1].asWaypointElement(), waypoint.asWaypointElement(), color(isActive(), edgeColor)))
            }
        }

        return mapOf(elementId to AreaWithZindex(area, AreaType.EDGE, waypointAnchors(ctx.canvas.camera)))
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?) {
        TODO("Not yet implemented")
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return edge.waypoint.filter { it.physical }.map { Point2D.Float(it.x, it.y) }.toMutableSet()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        val minX = edge.waypoint.minBy { it.x }?.x ?: 0.0f
        val minY = edge.waypoint.minBy { it.y }?.y ?: 0.0f
        val maxX = edge.waypoint.maxBy { it.x }?.x ?: 0.0f
        val maxY = edge.waypoint.maxBy { it.y }?.y ?: 0.0f

        return Rectangle2D.Float(
                minX,
                minY,
                maxX - minX,
                maxY - minY
        )
    }
}