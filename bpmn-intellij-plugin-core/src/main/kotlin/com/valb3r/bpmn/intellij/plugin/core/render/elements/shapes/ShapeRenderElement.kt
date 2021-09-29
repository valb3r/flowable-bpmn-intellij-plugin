package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.LocationUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.EdgeExtractionAnchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.internal.CascadeTranslationOrChangesToWaypoint
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.RectangleTransformationIntrospection
import com.valb3r.bpmn.intellij.plugin.core.state.CurrentState
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

const val WAYPOINT_OCCUPY_EPSILON = 1.0f

abstract class ShapeRenderElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        protected val shape: ShapeElement,
        state: () -> RenderState
) : BaseBpmnRenderElement(elementId, bpmnElementId, state) {

    protected val cascadeTo: Set<CascadeTranslationOrChangesToWaypoint>
    protected val edgeExtractionAnchor: EdgeExtractionAnchor

    init {
        edgeExtractionAnchor = computeAnchorLocation(elementId, state)
        cascadeTo = computeCascadables()
    }

    override val children: MutableList<BaseDiagramRenderElement> = mutableListOf(edgeExtractionAnchor)

    val shapeElem: ShapeElement
        get() = shape

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val elem = state().currentState.elementByDiagramId[shape.id]
        val props = state().currentState.elemPropertiesByStaticElementId[elem]
        val name = props?.get(PropertyType.NAME)?.value as String?

        state().ctx.interactionContext.dragEndCallbacks[elementId] = {
            dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex> -> onDragEnd(dx, dy, droppedOn, allDroppedOnAreas)
        }

        val shapeCtx = ShapeCtx(shape.id, elem, currentOnScreenRect(ctx.canvas.camera), props, name)
        if (state().history.contains(bpmnElementId)) {
            val indexes = state().history.mapIndexed {pos, id -> if (id == bpmnElementId) pos else null}.filterNotNull()
            state().ctx.canvas.drawTextNoCameraTransform(
                    Point2D.Float(shapeCtx.shape.x, shapeCtx.shape.y), indexes.toString(), Colors.INNER_TEXT_COLOR.color, Colors.DEBUG_ELEMENT_COLOR.color
            )
        }

        detectAndRenderNewSequenceAnchorMove()
        return doRender(ctx, shapeCtx)
    }

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val delId = elementId.elemIdToRemove()
        val deleteIconArea = state().ctx.canvas.drawIcon(BoundsElement(x, y, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state().icons.recycleBin)
        state().ctx.interactionContext.clickCallbacks[delId] = { dest ->
            dest.addElementRemovedEvent(getEventsToDeleteDiagram(), getEventsToDeleteElement())
        }

        return mutableMapOf(
                delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
        )
    }


    abstract fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex>

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        val compensated = compensateExpansionViewForDrag(dx, dy)
        // Avoid double dragging by cascade and then by children
        val emptySortedMap = linkedMapOf<BpmnElementId, AreaWithZindex>() // Quirk to create sorted map without comparable key
        val result = doOnDragEndWithoutChildren(compensated.x, compensated.y, null, allDroppedOnAreas)
        val alreadyDraggedLocations = result.filterIsInstance<LocationUpdateWithId>().map { it.diagramElementId }.toMutableSet()
        children.forEach {
            for (event in it.onDragEnd(compensated.x, compensated.y, null, emptySortedMap)) { // Children do not change parent - sortedMapOf()
                handleChildDrag(event, alreadyDraggedLocations, result)
            }
        }

        viewTransform = state().baseTransform
        return result
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        val events = mutableListOf<Event>()
        events += DraggedToEvent(elementId, dx, dy, null, null)
        val cascadeTargets = cascadeTo.filter { target -> target.cascadeSource == shape.bpmnElement } // TODO check if this comparison is still needed
        events += cascadeTargets
                .map { cascadeTo -> DraggedToEvent(cascadeTo.waypointId, dx, dy, cascadeTo.parentEdgeId, cascadeTo.internalId) }

        if (allDroppedOnAreas.isEmpty()) {
            return events
        }

        events += handlePossibleNestingTo(allDroppedOnAreas, cascadeTargets)

        return events
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun afterStateChangesAppliedNoChildren() {
        if (viewTransform is NullViewTransform) {
            return
        }

        cascadeTo.mapNotNull { state().elemMap[it.waypointId] }.forEach {
            it.viewTransform = viewTransform
        }
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Anchor(Point2D.Float(cx - halfWidth, cy), 10),
                Anchor(Point2D.Float(cx + halfWidth, cy), 10),
                Anchor(Point2D.Float(cx, cy - halfHeight), 10),
                Anchor(Point2D.Float(cx, cy + halfHeight), 10),

                Anchor(Point2D.Float(cx - halfWidth / 2.0f, cy - halfHeight)),
                Anchor(Point2D.Float(cx + halfWidth / 2.0f, cy - halfHeight)),
                Anchor(Point2D.Float(cx - halfWidth / 2.0f, cy + halfHeight)),
                Anchor(Point2D.Float(cx + halfWidth / 2.0f, cy + halfHeight)),

                Anchor(Point2D.Float(cx - halfWidth, cy - halfHeight / 2.0f)),
                Anchor(Point2D.Float(cx - halfWidth, cy + halfHeight / 2.0f)),
                Anchor(Point2D.Float(cx + halfWidth, cy - halfHeight / 2.0f)),
                Anchor(Point2D.Float(cx + halfWidth, cy + halfHeight / 2.0f))
        )
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(Anchor(Point2D.Float(cx, cy)))
    }

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        return viewTransform.transform(elementId, RectangleTransformationIntrospection(shape.rectBounds(), AreaType.SHAPE))
    }

    override fun currentRect(): Rectangle2D.Float {
        return shape.rectBounds()
    }

    protected open fun handlePossibleNestingTo(allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>, cascadeTargets: List<CascadeTranslationOrChangesToWaypoint>): MutableList<Event> {
        val allDroppedOn = linkedMapOf<AreaType, BpmnElementId>()
        allDroppedOnAreas.forEach { if (!allDroppedOn.containsKey(it.value.areaType)) allDroppedOn[it.value.areaType] = it.key}
        val nests = allDroppedOn[AreaType.SHAPE_THAT_NESTS]
        val parentProcess = allDroppedOn[AreaType.PARENT_PROCESS_SHAPE]
        val currentParent = parents.firstOrNull()
        val newEvents = mutableListOf<Event>()

        if (allDroppedOn[allDroppedOn.keys.first()] == currentParent?.bpmnElementId) {
            return newEvents
        }

        if (null != nests && nests != currentParent?.bpmnElementId) {
            newEvents += BpmnParentChangedEvent(shape.bpmnElement, nests)
            // Cascade parent change to waypoint owning edge
            newEvents += cascadeTargets.mapNotNull { state().currentState.elementByDiagramId[it.parentEdgeId] }.map { BpmnParentChangedEvent(it, nests) }

        } else if (null != parentProcess && parentProcess != parents.firstOrNull()?.bpmnElementId) {
            newEvents += BpmnParentChangedEvent(shape.bpmnElement, parentProcess)
            // Cascade parent change to waypoint owning edge
            newEvents += cascadeTargets.mapNotNull { state().currentState.elementByDiagramId[it.parentEdgeId] }.map { BpmnParentChangedEvent(it, parentProcess) }
        }
        return newEvents
    }

    protected fun computeCascadables(): Set<CascadeTranslationOrChangesToWaypoint> {
        val idCascadesTo = setOf(PropertyType.SOURCE_REF, PropertyType.TARGET_REF)
        val result = mutableSetOf<CascadeTranslationOrChangesToWaypoint>()
        val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
        state().currentState.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
        state().currentState.elemPropertiesByStaticElementId.forEach { (owner, props) ->
            idCascadesTo.intersect(props.keys).filter { props[it]?.value == shape.bpmnElement.id }.forEach { type ->
                when (state().currentState.elementByBpmnId[owner]?.element) {
                    is BpmnSequenceFlow -> { result += computeCascadeToWaypoint(state().currentState, shape.bpmnElement, owner, type) }
                }
            }

        }
        return result
    }

    protected fun computeCascadeToWaypoint(state: CurrentState, cascadeTrigger: BpmnElementId, owner: BpmnElementId, type: PropertyType): Collection<CascadeTranslationOrChangesToWaypoint> {
        return state.edges
                .filter { it.bpmnElement == owner }
                .map {
                    val index = if (type == PropertyType.SOURCE_REF) 0 else it.waypoint.size - 1
                    val waypoint = it.waypoint[index]
                    CascadeTranslationOrChangesToWaypoint(cascadeTrigger, waypoint.id, Point2D.Float(waypoint.x, waypoint.y), it.id, waypoint.internalPhysicalPos)
                }
    }

    protected open fun parentForRelatedSequenceElem(): BaseBpmnRenderElement {
        return parents.first()
    }

    private fun detectAndRenderNewSequenceAnchorMove() {
        val expected = viewTransform.transform(elementId, edgeExtractionAnchor.location)
        if (expected.distance(edgeExtractionAnchor.transformedLocation) > EPSILON && edgeExtractionAnchor.isActiveOrDragged()) {
            renderNewSequenceAnchorMove()
        }
    }

    private fun renderNewSequenceAnchorMove() {
        val bounds = currentOnScreenRect(state().ctx.canvas.camera)
        state().ctx.canvas.drawLineWithArrow(
                Point2D.Float(bounds.centerX.toFloat(), bounds.centerY.toFloat()),
                edgeExtractionAnchor.transformedLocation,
                Colors.ARROW_COLOR.color
        )
    }

    private fun handleChildDrag(event: Event, alreadyDraggedLocations: MutableSet<DiagramElementId>, result: MutableList<Event>) {
        if (event !is LocationUpdateWithId) {
            result += event
            return
        }

        if (alreadyDraggedLocations.contains(event.diagramElementId)) {
            return
        }

        alreadyDraggedLocations += event.diagramElementId
        result += event
    }

    private fun computeAnchorLocation(currentElementId: DiagramElementId, state: () -> RenderState): EdgeExtractionAnchor {
        return EdgeExtractionAnchor(
                DiagramElementId("NEW-SEQUENCE:" + shape.id.id),
                currentElementId,
                actionsAnchorTopEnd(Rectangle2D.Float(
                        shape.bounds().first.x,
                        shape.bounds().first.y,
                        shape.bounds().second.x - shape.bounds().first.x,
                        shape.bounds().second.y - shape.bounds().first.y
                )),
                { droppedOn, allDroppedOn -> onWaypointAnchorDragEnd(droppedOn, allDroppedOn) },
                state
        )
    }

    private fun onWaypointAnchorDragEnd(droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        val targetArea = allDroppedOnAreas[droppedOn]
        if (null == droppedOn || null == targetArea) {
            return mutableListOf()
        }

        val elem = state().currentState.elementByBpmnId[bpmnElementId] ?: return mutableListOf()

        val newSequenceBpmn = newElementsFactory(state().ctx.project).newOutgoingSequence(elem.element)
        val anchors = findSequenceAnchors(targetArea) ?: return mutableListOf()
        val notYetExistingDiagramId = DiagramElementId("")
        val sourceBounds = shape.rectBounds()
        val firstAnchorCompensated = compensateExpansionViewOnLocation(notYetExistingDiagramId, anchors.first, Point2D.Float(sourceBounds.centerX.toFloat(), sourceBounds.centerY.toFloat()))
        val secondAnchorCompensated = compensateExpansionViewOnLocation(notYetExistingDiagramId, anchors.second, anchors.second)
        val newSequenceDiagram = newElementsFactory(state().ctx.project).newDiagramObject(EdgeElement::class, newSequenceBpmn)
                .copy(waypoint = listOf(
                        WaypointElement(firstAnchorCompensated.x, firstAnchorCompensated.y),
                        WaypointElement(secondAnchorCompensated.x, secondAnchorCompensated.y)
                ))

        val props = newElementsFactory(state().ctx.project).propertiesOf(newSequenceBpmn).toMutableMap()
        props[PropertyType.TARGET_REF] = Property(droppedOn.id)

        return mutableListOf(
                BpmnEdgeObjectAddedEvent(
                        WithParentId(parentForRelatedSequenceElem().bpmnElementId, newSequenceBpmn),
                        EdgeElementState(newSequenceDiagram),
                        props
                )
        )
    }

    private fun findSequenceAnchors(droppedOnTarget: AreaWithZindex): Pair<Point2D.Float, Point2D.Float>? {
        val allStartWaypointsAnchors = waypointAnchors(state().ctx.canvas.camera)
        val allEndWaypointsAnchors = droppedOnTarget.anchorsForWaypoints

        val fromCenters = findBestSequenceElement(
                allStartWaypointsAnchors.filter { it.priority > 0 }.toMutableSet(),
                allEndWaypointsAnchors.filter { it.priority > 0 }.toMutableSet(),
                droppedOnTarget,
                false
        )

        if (null != fromCenters) {
            return fromCenters
        }

        return findBestSequenceElement(allStartWaypointsAnchors, allEndWaypointsAnchors, droppedOnTarget, true)
    }

    private fun findBestSequenceElement(
            allStartWaypointsAnchors: MutableSet<Anchor>,
            allEndWaypointsAnchors: MutableSet<Anchor>,
            droppedOnTarget: AreaWithZindex,
            allowShapeIntersection: Boolean
    ): Pair<Point2D.Float, Point2D.Float>? {
        if (allStartWaypointsAnchors.isEmpty() || allEndWaypointsAnchors.isEmpty()) {
            return null
        }

        var startAvailable = allStartWaypointsAnchors.filter { !isAnchorOccupated(it.point) }
        var endAvailable = allEndWaypointsAnchors.filter { !isAnchorOccupated(it.point) }

        if (startAvailable.isEmpty()) {
            startAvailable = allStartWaypointsAnchors.toList()
        }

        if (endAvailable.isEmpty()) {
            endAvailable = allEndWaypointsAnchors.toList()
        }

        val doesNotIntersectArea = { anchor: Pair<Anchor, Anchor> -> Boolean
            val current = currentOnScreenRect(state().ctx.canvas.camera)
            val line = Line2D.Float(anchor.first.point, anchor.second.point)
            !line.intersects(current) && !line.intersects(droppedOnTarget.area.bounds2D)
        }

        return cartesianProduct(startAvailable, endAvailable)
            .filter { if (allowShapeIntersection) true else doesNotIntersectArea(it) }
            .minBy { it: Pair<Anchor, Anchor> -> it.first.point.distance(it.second.point) }
            ?.let { Pair(it.first.point, it.second.point) }
    }

    private fun isAnchorOccupated(anchor: Point2D.Float): Boolean {
        state().currentState.edges.forEach {
            if (anchor.distance(Point2D.Float(it.waypoint[0].x, it.waypoint[0].y)) < WAYPOINT_OCCUPY_EPSILON) {
                return true
            }

            if (anchor.distance(Point2D.Float(it.waypoint[it.waypoint.size - 1].x, it.waypoint[it.waypoint.size - 1].y)) < WAYPOINT_OCCUPY_EPSILON) {
                return true
            }
        }
        return false
    }

    fun <T, U> cartesianProduct(first: Collection<T>, second: Collection<U>): Sequence<Pair<T, U>> {
        return first.asSequence().flatMap { lhsElem -> second.asSequence().map { rhsElem -> lhsElem to rhsElem } }
    }
}