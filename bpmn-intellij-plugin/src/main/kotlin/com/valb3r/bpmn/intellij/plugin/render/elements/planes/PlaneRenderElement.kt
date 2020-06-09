package com.valb3r.bpmn.intellij.plugin.render.elements.planes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.render.*
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.Rectangle
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

val viewMin = Float.MIN_VALUE
val viewMax = Float.MAX_VALUE

class PlaneRenderElement(
        override val elementId: DiagramElementId,
        private val bpmnElementId: BpmnElementId,
        state: RenderState,
        override val children: MutableList<BaseRenderElement> = mutableListOf()
): BaseRenderElement(elementId, state) {

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        return mutableListOf()
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        // NOP
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        return mutableListOf()
    }

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        return Rectangle2D.Float()
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val area = InfiniteShape()
        return mutableMapOf(elementId to AreaWithZindex(area, AreaType.SHAPE_THAT_NESTS, mutableSetOf(), mutableSetOf(), bpmnElementId = bpmnElementId, index = OWNING_PROCESS_Z_INDEX))
    }

    override fun drawActions(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        return mutableMapOf()
    }
}

private class InfiniteShape: Area() {

    override fun contains(x: Double, y: Double): Boolean {
        return true
    }

    override fun contains(p: Point2D?): Boolean {
        return true
    }

    override fun contains(x: Double, y: Double, w: Double, h: Double): Boolean {
        return true
    }

    override fun contains(r: Rectangle2D?): Boolean {
        return true
    }

    override fun getBounds2D(): Rectangle2D {
        return Rectangle()
    }

    override fun intersects(x: Double, y: Double, w: Double, h: Double): Boolean {
        return true
    }

    override fun intersects(r: Rectangle2D?): Boolean {
        return true
    }

    override fun getBounds(): Rectangle {
        return Rectangle()
    }
}