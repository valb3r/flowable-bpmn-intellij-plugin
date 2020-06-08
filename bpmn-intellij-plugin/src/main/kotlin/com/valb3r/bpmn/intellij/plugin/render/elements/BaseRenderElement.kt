package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.DragViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ViewTransform
import java.awt.Color
import java.awt.Event
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

abstract class BaseRenderElement(
        protected open val elementId: DiagramElementId,
        protected val state: RenderState,
        protected open var viewTransform: ViewTransform = NullViewTransform()
) {

    var isVisible: Boolean? = null

    open val children: MutableList<BaseRenderElement> = mutableListOf()

    open fun isVisible(): Boolean {
        return true == isVisible
    }

    open fun isActive(): Boolean {
        return state.ctx.selectedIds.contains(elementId)
    }

    open fun isDragged(): Boolean {
        return state.ctx.interactionContext.draggedIds.contains(elementId)
    }

    open fun isActiveOrDragged(): Boolean {
        return isActive() || isDragged()
    }

    open fun render(ctx: RenderContext): MutableMap<DiagramElementId, AreaWithZindex> {
        propagateActivityStateToChildren()
        propagateDragging(ctx)
        val result = doRender(ctx).toMutableMap()
        children.forEach { result += it.render(ctx) }
        return result
    }

    open fun dragTo(dx: Float, dy: Float) {
        viewTransform = DragViewTransform(dx, dy)
        doDragToWithoutChildren(dx, dy)
        children.forEach { it.dragTo(dx, dy) }
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

    protected fun propagateActivityStateToChildren() {
        if (isActive()) {
            children.forEach { it.isVisible = true }
        } else {
            children.forEach { it.isVisible = it.isActiveOrDragged() }
        }
    }

    protected open fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.SELECTED_COLOR.color else color.color
    }

    abstract fun doDragToWithoutChildren(dx: Float, dy: Float)
    abstract fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>

    abstract fun doResizeWithoutChildren(dw: Float, dh: Float)
    abstract fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event>

    protected abstract fun currentRect(camera: Camera): Rectangle2D.Float

    protected abstract fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float>
    protected abstract fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float>

    protected abstract fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex>

    protected fun propagateDragging(ctx: RenderContext) {
        if (isActiveOrDragged()) {
            val dx = ctx.interactionContext.dragCurrent.x - ctx.interactionContext.dragStart.x
            val dy = ctx.interactionContext.dragCurrent.y - ctx.interactionContext.dragStart.y
            if (abs(dx) + abs(dy) != 0.0f) {
                dragTo(dx, dy)
            }
        }
        children.forEach { it.propagateDragging(ctx) }
    }
}