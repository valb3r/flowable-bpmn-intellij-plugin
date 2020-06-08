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

val EPSILON = 0.1f

abstract class BaseRenderElement(
        open val elementId: DiagramElementId,
        protected val state: RenderState,
        internal open var viewTransform: ViewTransform = NullViewTransform()
) {

    var isVisible: Boolean? = null

    open val children: MutableList<BaseRenderElement> = mutableListOf()

    open fun multipleElementsSelected(): Boolean {
        return state.ctx.selectedIds.size > 1
    }

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

    open fun applyContextChanges(elemMap: Map<DiagramElementId, BaseRenderElement>) {
        propagateActivityStateToChildren()

        val dx = state.ctx.interactionContext.dragCurrent.x - state.ctx.interactionContext.dragStart.x
        val dy = state.ctx.interactionContext.dragCurrent.y - state.ctx.interactionContext.dragStart.y

        if (abs(dx) + abs(dy) > EPSILON) {
            propagateDragging(state.ctx, dx, dy)
        }

        propagateStateChangesApplied(elemMap)
    }

    open fun render(): MutableMap<DiagramElementId, AreaWithZindex> {
        val result = doRenderWithoutChildren(state.ctx).toMutableMap()
        children.forEach { result += it.render() }
        return result
    }

    open fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>  {
        val result = doOnDragEndWithoutChildren(dx, dy, droppedOn)
        children.forEach { result += it.onDragEnd(dx, dy, droppedOn) }
        return result
    }

    protected open fun dragTo(dx: Float, dy: Float) {
        viewTransform = DragViewTransform(dx, dy)
        doDragToWithoutChildren(dx, dy)
        children.forEach { it.dragTo(dx, dy) }
    }

    protected fun propagateActivityStateToChildren() {
        if (isActive()) {
            isVisible = true
            children.forEach { it.isVisible = true }
        } else {
            children.forEach { it.propagateActivityStateToChildren() }
        }
    }

    protected open fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.SELECTED_COLOR.color else color.color
    }

    /**
     * Allows parent elements to handle children updates and react on them.
     */
    protected open fun propagateStateChangesApplied(elemMap: Map<DiagramElementId, BaseRenderElement>) {
        afterStateChangesAppliedNoChildren(elemMap)
        children.forEach { it.propagateStateChangesApplied(elemMap) }
    }

    /**
     * Allows parent elements to handle children updates and react on them.
     */
    protected open fun afterStateChangesAppliedNoChildren(elemMap: Map<DiagramElementId, BaseRenderElement>) {
    }

    abstract fun doDragToWithoutChildren(dx: Float, dy: Float)
    abstract fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>

    abstract fun doResizeWithoutChildren(dw: Float, dh: Float)
    abstract fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event>

    protected abstract fun currentRect(camera: Camera): Rectangle2D.Float

    protected abstract fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float>
    protected abstract fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float>

    protected abstract fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex>

    protected fun propagateDragging(ctx: RenderContext, dx: Float, dy: Float) {
        if (!isActiveOrDragged()) {
            children.filter { !it.needsDirectParentActiveToAcceptEvents() }.forEach { it.propagateDragging(ctx, dx, dy) }
            return
        }

        dragTo(dx, dy)
        children.forEach { it.propagateDragging(ctx, dx, dy) }
    }

    protected open fun needsDirectParentActiveToAcceptEvents(): Boolean {
        return false
    }

    protected open fun acceptsInternalEvents(): Boolean {
        return true
    }
}