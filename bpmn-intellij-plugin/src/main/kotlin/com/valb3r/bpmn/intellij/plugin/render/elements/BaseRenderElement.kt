package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.DragViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ViewTransform
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

val EPSILON = 0.1f
private val ACTION_AREA_STROKE = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(2.0f), 0.0f)
const val ACTIONS_ICO_SIZE = 15f
private const val actionsMargin = 5f

abstract class BaseRenderElement(
        open val elementId: DiagramElementId,
        protected val state: RenderState,
        internal open var viewTransform: ViewTransform = NullViewTransform()
) {

    var isVisible: Boolean? = null

    open val children: MutableList<BaseRenderElement> = mutableListOf()

    /**
     * Parents in the order: direct parent, parent of direct parent...
     * Typically 'direct parent' is sufficient and is only provided
     */
    open val parents: MutableList<BpmnElementId> = mutableListOf()

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

    open fun applyContextChanges() {
        propagateActivityStateToChildren()

        val dx = state.ctx.interactionContext.dragCurrent.x - state.ctx.interactionContext.dragStart.x
        val dy = state.ctx.interactionContext.dragCurrent.y - state.ctx.interactionContext.dragStart.y

        if (abs(dx) + abs(dy) > EPSILON) {
            propagateDragging(state.ctx, dx, dy)
        }

        propagateStateChangesApplied()
    }

    open fun render(): MutableMap<DiagramElementId, AreaWithZindex> {
        val result = doRenderWithoutChildren(state.ctx).toMutableMap()
        children.forEach { result += it.render() }
        if (isActive() && !multipleElementsSelected()) {
            result += drawActionsElement()
        }

        return result
    }

    open fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event>  {
        val result = doOnDragEndWithoutChildren(dx, dy, droppedOn)
        children.forEach { result += it.onDragEnd(dx, dy, droppedOn) }
        viewTransform = NullViewTransform()
        return result
    }

    protected fun actionsRect(): Rectangle2D.Float {
        val rect = currentRect(state.ctx.canvas.camera)
        return Rectangle2D.Float(rect.x - actionsMargin, rect.y - actionsMargin, rect.width + 2.0f * actionsMargin, rect.height + 2.0f * actionsMargin)
    }

    protected fun drawActionsElement(): Map<DiagramElementId, AreaWithZindex> {
        val rect = actionsRect()
        state.ctx.canvas.drawRectNoFill(Point2D.Float(rect.x, rect.y), rect.width, rect.height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        return drawActions(rect.x + rect.width + actionsMargin, rect.y)
    }

    protected abstract fun drawActions(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex>

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
    protected open fun propagateStateChangesApplied() {
        afterStateChangesAppliedNoChildren()
        children.forEach { it.propagateStateChangesApplied() }
    }

    /**
     * Allows parent elements to handle children updates and react on them.
     */
    protected open fun afterStateChangesAppliedNoChildren() {
    }

    internal open fun doComputeLocationChangesBasedOnTransformationWithCascade(): MutableList<Event> {
        return mutableListOf()
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
            children.forEach { it.propagateDragging(ctx, dx, dy) }
            return
        }

        dragTo(dx, dy)
        children.forEach { it.propagateDragging(ctx, dx, dy) }
    }

    protected open fun acceptsInternalEvents(): Boolean {
        return true
    }
}