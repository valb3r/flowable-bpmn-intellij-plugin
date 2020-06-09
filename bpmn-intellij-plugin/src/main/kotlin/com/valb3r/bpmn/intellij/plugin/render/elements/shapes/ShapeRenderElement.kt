package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.render.*
import com.valb3r.bpmn.intellij.plugin.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.internal.CascadeTranslationToWaypoint
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

private const val WAYPOINT_LEN = 40.0f

abstract class ShapeRenderElement(
        override val elementId: DiagramElementId,
        protected val shape: ShapeElement,
        state: RenderState
) : BaseRenderElement(elementId, state) {

    protected open val cascadeTo = computeCascadables()

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val elem = state.currentState.elementByDiagramId[shape.id]
        val props = state.currentState.elemPropertiesByStaticElementId[elem]
        val name = props?.get(PropertyType.NAME)?.value as String?

        state.ctx.interactionContext.dragEndCallbacks[elementId] = {
            dx: Float, dy: Float, id: BpmnElementId? -> onDragEnd(dx, dy, id)
        }

        return doRender(ctx, ShapeCtx(shape.id, elem, currentRect(ctx.canvas.camera), props, name))
    }

    override fun drawActions(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        var currY = y
        val delId = DiagramElementId("DEL:$elementId")
        val deleteIconArea = state.ctx.canvas.drawIcon(BoundsElement(x, currY, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state.icons.recycleBin)
        state.ctx.interactionContext.clickCallbacks[delId] = { dest ->
            dest.addElementRemovedEvent(listOf(DiagramElementRemovedEvent(elementId)), listOf(BpmnElementRemovedEvent(shape.bpmnElement)))
        }
        currY += ACTIONS_ICO_SIZE * 2.0f

        val newLinkId = DiagramElementId("NEWLINK:$elementId")
        val newLinkArea = state.ctx.canvas.drawIcon(BoundsElement(x, currY, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state.icons.sequence)
        state.ctx.interactionContext.clickCallbacks[newLinkId] = { dest ->
            state.currentState.elementByBpmnId[shape.bpmnElement]?.let { it: WithParentId ->
                val newSequenceBpmn = newElementsFactory().newOutgoingSequence(it.element)
                val bounds = currentRect(state.ctx.canvas.camera)
                val width = bounds.width
                val height = bounds.height

                val newSequenceDiagram = newElementsFactory().newDiagramObject(EdgeElement::class, newSequenceBpmn)
                        .copy(waypoint = listOf(
                                WaypointElement(bounds.x + width, bounds.y + height / 2.0f),
                                WaypointElement(bounds.x + width + WAYPOINT_LEN, bounds.y + height / 2.0f)
                        ))
                dest.addObjectEvent(
                        BpmnEdgeObjectAddedEvent(
                                WithParentId(parents.first(), newSequenceBpmn),
                                EdgeElementState(newSequenceDiagram),
                                newElementsFactory().propertiesOf(newSequenceBpmn)
                        )
                )
            }
        }

        return mutableMapOf(
                delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, elementId),
                newLinkId to AreaWithZindex(newLinkArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, elementId)
        )
    }

    abstract fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex>

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        val events = mutableListOf<Event>()
        events += DraggedToEvent(elementId, dx, dy, null, null)
        events += cascadeTo
                .filter { target -> target.cascadeSource == shape.bpmnElement }
                .filter { target -> !state.ctx.interactionContext.draggedIds.contains(target.waypointId) }
                .map { cascadeTo -> DraggedToEvent(cascadeTo.waypointId, dx, dy, cascadeTo.parentEdgeId, cascadeTo.internalId) }
        return events
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun afterStateChangesAppliedNoChildren(elemMap: Map<DiagramElementId, BaseRenderElement>) {
        if (viewTransform is NullViewTransform) {
            return
        }

        cascadeTo.mapNotNull { elemMap[it.waypointId] }.forEach {
            it.viewTransform = viewTransform
        }
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        val rect = currentRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy),

                Point2D.Float(cx - halfWidth, cy),
                Point2D.Float(cx + halfWidth, cy),
                Point2D.Float(cx, cy - halfHeight),
                Point2D.Float(cx, cy + halfHeight),

                Point2D.Float(cx - halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx - halfWidth / 2.0f, cy + halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy + halfHeight),

                Point2D.Float(cx - halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx - halfWidth, cy + halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy + halfHeight / 2.0f)
        )
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        val rect = currentRect(camera)
        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy)
        )
    }

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        return viewTransform.transform(shape.rectBounds())
    }

    protected fun computeCascadables(): Set<CascadeTranslationToWaypoint> {
        val idCascadesTo = setOf(PropertyType.SOURCE_REF, PropertyType.TARGET_REF)
        val result = mutableSetOf<CascadeTranslationToWaypoint>()
        val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
        state.currentState.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
        state.currentState.elemPropertiesByStaticElementId.forEach { (owner, props) ->
            idCascadesTo.intersect(props.keys).filter { props[it]?.value == shape.bpmnElement.id }.forEach { type ->
                when (state.currentState.elementByBpmnId[owner]?.element) {
                    is BpmnSequenceFlow -> { result += computeCascadeToWaypoint(state.currentState, shape.bpmnElement, owner, type) }
                }
            }

        }
        return result
    }

    protected fun computeCascadeToWaypoint(state: CurrentState, cascadeTrigger: BpmnElementId, owner: BpmnElementId, type: PropertyType): Collection<CascadeTranslationToWaypoint> {
        return state.edges
                .filter { it.bpmnElement == owner }
                .map {
                    val index = if (type == PropertyType.SOURCE_REF) 0 else it.waypoint.size - 1
                    val waypoint = it.waypoint[index]
                    CascadeTranslationToWaypoint(cascadeTrigger, waypoint.id, Point2D.Float(waypoint.x, waypoint.y), it.id, waypoint.internalPhysicalPos)
                }
    }
}