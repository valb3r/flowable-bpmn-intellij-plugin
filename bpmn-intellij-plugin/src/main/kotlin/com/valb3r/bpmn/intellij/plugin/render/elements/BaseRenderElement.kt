package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.Color
import java.awt.Event
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class BaseRenderElement(
        protected open val elementId: DiagramElementId,
        protected val state: CurrentState
) {
    open val children: MutableList<BaseRenderElement> = mutableListOf()

    open fun isActive(): Boolean {
        return false
    }

    open fun isDragged(): Boolean {
        return false
    }

    open fun isActiveOrDragged(): Boolean {
        return isActive() || isDragged()
    }

    open fun render(ctx: RenderContext): MutableMap<DiagramElementId, AreaWithZindex> {
        val result = doRender(ctx).toMutableMap()
        children.forEach { result += it.render(ctx) }
        return result
    }

    open fun dragTo(dx: Float, dy: Float, droppedOn: BpmnElementId?) {
        doDragToWithoutChildren(dx, dy, droppedOn)
        children.forEach { it.dragTo(dx, dy, droppedOn) }
    }

    open fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>  {
        val result = doOnDragEndWithoutChildren(dx, dy, droppedOn)
        children.forEach { result += it.onDragEnd(dx, dy, droppedOn) }
        return result
    }

    open fun resize(camera: Camera, dw: Float, dh: Float, droppedOn: BpmnElementId?) {
        val iniRect = currentRect(camera)
        doResizeWithoutChildren(dw, dh)
        val currRect = currentRect(camera)
        children.forEach { it.resize(camera, dw * currRect.width / iniRect.width, dh * currRect.height / iniRect.height, droppedOn) }
    }

    open fun onResizeEnd(camera: Camera, dw: Float, dh: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        val iniRect = currentRect(camera)
        val result = doResizeEndWithoutChildren(dw, dh)
        val currRect = currentRect(camera)
        children.forEach { result += it.onResizeEnd(camera, dw * currRect.width / iniRect.width, dh * currRect.height / iniRect.height, droppedOn) }
        return result
    }

    protected open fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.SELECTED_COLOR.color else color.color
    }

    abstract fun doDragToWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?)
    abstract fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>

    abstract fun doResizeWithoutChildren(dw: Float, dh: Float)
    abstract fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event>

    protected abstract fun currentRect(camera: Camera): Rectangle2D.Float
    protected abstract fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float>
    protected abstract fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float>

    protected abstract fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex>
}