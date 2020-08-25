package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.render.ANCHOR_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.DragViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ExpandViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.PreTransformHandler
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

fun DiagramElementId.elemIdToRemove(): DiagramElementId {
    return DiagramElementId("DEL:" + this.id)
}

abstract class BaseDiagramRenderElement(
        open val elementId: DiagramElementId,
        protected open val state: RenderState,
        internal open var viewTransform: ViewTransform = state.baseTransform
) {

    var isVisible: Boolean? = null

    open val children: MutableList<BaseDiagramRenderElement> = mutableListOf()

    /**
     * Parents in the order: direct parent, parent of direct parent...
     * Typically 'direct parent' is sufficient and is only provided
     */
    open val parents: MutableList<BaseBpmnRenderElement> = mutableListOf()

    open fun multipleElementsSelected(): Boolean {
        return state.ctx.selectedIds.size > 1
    }

    open fun isVisible(): Boolean {
        return true == isVisible
    }

    open fun isActive(): Boolean {
        return state.ctx.selectedIds.contains(elementId)
    }

    open fun isTargetedByDrag(): Boolean {
        return state.ctx.interactionContext.dragTargetedIds.contains(elementId)
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
        children.sortedBy { it.zIndex() }.forEach { result += it.render() }
        if (isActive() && !multipleElementsSelected()) {
            result += drawActionsElement()
        }

        return result
    }

    open fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event>  {
        val result = doOnDragEndWithoutChildren(dx, dy, droppedOn, allDroppedOnAreas)
        children.forEach { result += it.onDragEnd(dx, dy, droppedOn, allDroppedOnAreas) }
        viewTransform = state.baseTransform
        return result
    }

    open fun getEventsToDeleteDiagram(): List<DiagramElementRemovedEvent> {
        val delete = mutableListOf<DiagramElementRemovedEvent>()
        children.forEach {delete += it.getEventsToDeleteDiagram()}
        delete += DiagramElementRemovedEvent(elementId)
        return delete
    }

    open fun getEventsToDeleteElement(): List<BpmnElementRemovedEvent> {
        return listOf()
    }

    open fun zIndex(): Int {
        return if (isActiveOrDragged()) ANCHOR_Z_INDEX else (parents.firstOrNull()?.zIndex() ?: -1) + 1
    }

    open fun enumerateChildrenRecursively() : List<BaseDiagramRenderElement> {
        val result = mutableListOf<BaseDiagramRenderElement>()
        result += children.flatMap { rootAndEnumerateChildrenRecursively(it) }
        return result
    }

    protected open fun rootAndEnumerateChildrenRecursively(root: BaseDiagramRenderElement) : List<BaseDiagramRenderElement> {
        val result = mutableListOf<BaseDiagramRenderElement>()
        result += root
        result += root.children.flatMap { rootAndEnumerateChildrenRecursively(it) }
        return result
    }

    protected fun actionsRect(shapeRect: Rectangle2D.Float): Rectangle2D.Float {
        return Rectangle2D.Float(shapeRect.x - actionsMargin, shapeRect.y - actionsMargin, shapeRect.width + 2.0f * actionsMargin, shapeRect.height + 2.0f * actionsMargin)
    }

    protected open fun drawActionsElement(): Map<DiagramElementId, AreaWithZindex> {
        val rect = actionsRect(currentOnScreenRect(state.ctx.canvas.camera))
        state.ctx.canvas.drawRectNoFill(Point2D.Float(rect.x, rect.y), rect.width, rect.height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        val rightActionsLocation = actionsAnchorRight(rect)
        return drawActionsRight(rightActionsLocation.x, rightActionsLocation.y).toMutableMap()
    }

    protected abstract fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex>

    protected open fun actionsAnchorRight(shapeRect: Rectangle2D.Float): Point2D.Float {
        val rect = actionsRect(shapeRect)
        return Point2D.Float(rect.x + rect.width, rect.y + 2.0f * actionsMargin)
    }

    protected open fun actionsAnchorTopEnd(shapeRect: Rectangle2D.Float): Point2D.Float {
        val rect = actionsRect(shapeRect)
        return Point2D.Float(rect.x + rect.width, rect.y)
    }

    protected open fun dragTo(dx: Float, dy: Float) {
        viewTransform = DragViewTransform(dx, dy, PreTransformHandler(mutableListOf(viewTransform)))
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

    protected open fun selectionColor(): Color {
        if (isActive()) {
            return Colors.SELECTED_COLOR.color
        }

        if (isTargetedByDrag()) {
            return Colors.DRAG_SELECTED_COLOR.color
        }

        return Colors.SELECTED_COLOR.color
    }

    protected open fun color(notSelectedColor: Colors): Color {
        if (isActive()) {
            return Colors.SELECTED_COLOR.color
        }

        if (isTargetedByDrag()) {
            return Colors.DRAG_SELECTED_COLOR.color
        }

        return notSelectedColor.color
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
    abstract fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event>

    abstract fun doResizeWithoutChildren(dw: Float, dh: Float)
    abstract fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event>

    protected abstract fun currentOnScreenRect(camera: Camera): Rectangle2D.Float

    protected abstract fun waypointAnchors(camera: Camera): MutableSet<Anchor>
    protected abstract fun shapeAnchors(camera: Camera): MutableSet<Anchor>

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

    protected open fun compensateExpandViewTransformForDragDelta(elemX: Float, elemY: Float, dx: Float, dy: Float): Point2D.Float {
        val toUndo = mutableListOf<ExpandViewTransform>()
        val transform = viewTransform
        if (transform is ExpandViewTransform) {
            toUndo += transform
        }

        toUndo += viewTransform.listTransformsOfType(ExpandViewTransform::class.java)

        var currentPosition = Point2D.Float(elemX + dx, elemY + dy)
        var startPosition = Point2D.Float(elemX, elemY)
        toUndo.forEach {
            currentPosition = it.undoTransform(elementId, Point2D.Float(currentPosition.x, currentPosition.y))
            startPosition = it.undoTransform(elementId, Point2D.Float(startPosition.x, startPosition.y))
        }
        return Point2D.Float(currentPosition.x - startPosition.x, currentPosition.y - startPosition.y)
    }
}