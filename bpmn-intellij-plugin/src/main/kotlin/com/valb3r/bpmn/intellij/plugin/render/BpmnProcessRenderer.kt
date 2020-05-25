package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.*
import com.valb3r.bpmn.intellij.plugin.events.*
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.nio.charset.StandardCharsets
import kotlin.math.min

class BpmnProcessRenderer {

    private val undoRedoStartMargin = 20.0f
    private val waypointLen = 40.0f
    private val activityToolBoxGap = 8.0f
    private val anchorRadius = 5f
    private val nodeRadius = 3f
    private val actionsIcoSize = 15f
    private val actionsMargin = 5f

    private val undoId = DiagramElementId("UNDO")
    private val redoId = DiagramElementId("REDO")

    private val UNDO = IconLoader.getIcon("/icons/actions/undo.png")
    private val REDO = IconLoader.getIcon("/icons/actions/redo.png")
    private val GEAR = IconLoader.getIcon("/icons/render/gear.png")
    private val EXCLUSIVE_GATEWAY = "/icons/exclusive-gateway.svg".asResource()!!
    private val SEQUENCE = "/icons/sequence.svg".asResource()!!
    private val RECYCLE_BIN = "/icons/recycle-bin.svg".asResource()!!

    private val ANCHOR_STROKE = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(5.0f), 0.0f)
    private val ACTION_AREA_STROKE = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(2.0f), 0.0f)

    fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val state = ctx.stateProvider.currentState()
        val areaByElement: MutableMap<DiagramElementId, AreaWithZindex> = HashMap()
        val renderMeta = RenderMetadata(
                ctx.interactionContext,
                ctx.selectedIds,
                state.elementByDiagramId,
                state.elementByBpmnId,
                state.elemPropertiesByStaticElementId,
                computeCascadables(ctx, state)
        )

        // Shapes are drawn first as they can cause cascaded location updates in edges
        dramBpmnElements(state.shapes, areaByElement, ctx.canvas, renderMeta)
        drawBpmnEdges(state.edges, areaByElement, ctx.canvas, renderMeta)
        ctx.interactionContext.anchorsHit?.apply { drawAnchorsHit(ctx.canvas, this) }

        drawUndoRedo(ctx, state, renderMeta, areaByElement)
        drawSelectionRect(ctx)
        return areaByElement
    }

    private fun drawSelectionRect(ctx: RenderContext) {
        ctx.interactionContext.dragSelectionRect?.let {
            val rect = it.toRect()
            ctx.canvas.drawRectNoCameraTransform(Point2D.Float(rect.x, rect.y), rect.width, rect.height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        }
    }

    private fun drawUndoRedo(ctx: RenderContext, state: CurrentState, meta: RenderMetadata, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>) {
        val start = Point2D.Float(undoRedoStartMargin, undoRedoStartMargin)
        var locationX = start.x
        val locationY = start.y

        if (state.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.UNDO)) {
            val color = if (isActive(undoId, meta)) Colors.SELECTED_COLOR else null
            val areaUndo = color?.let { ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), UNDO, it.color) }
                    ?: ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), UNDO)
            areaByElement[undoId] = AreaWithZindex(areaUndo, Point2D.Float(0.0f, 0.0f), AreaType.SHAPE)
            locationX += UNDO.iconWidth + undoRedoStartMargin
            ctx.interactionContext.clickCallbacks[undoId] = { dest -> dest.undo() }
        }

        if (state.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.REDO)) {
            val color = if (isActive(redoId, meta)) Colors.SELECTED_COLOR else null
            val areaRedo = color?.let { ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), REDO, it.color) }
                    ?: ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), REDO)
            areaByElement[redoId] = AreaWithZindex(areaRedo, Point2D.Float(0.0f, 0.0f), AreaType.SHAPE)
            locationX += REDO.iconWidth + undoRedoStartMargin
            ctx.interactionContext.clickCallbacks[redoId] = { dest -> dest.redo() }
        }
    }

    private fun computeCascadables(ctx: RenderContext, state: CurrentState): Set<CascadeTranslationToWaypoint> {
        val idCascadesTo = setOf(SOURCE_REF, TARGET_REF)
        val result = mutableSetOf<CascadeTranslationToWaypoint>()
        val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
        state.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
        ctx.interactionContext.draggedIds.mapNotNull { state.elementByDiagramId[it] }.filter { state.elementByBpmnId[it] !is BpmnSequenceFlow }.forEach { parent ->
            state.elemPropertiesByStaticElementId.forEach { (owner, props) ->
                idCascadesTo.intersect(props.keys).filter { props[it]?.value == parent.id }.forEach { type ->
                    when (state.elementByBpmnId[owner]) {
                        is BpmnSequenceFlow -> result += computeCascadeToWaypoint(state, parent, owner, type)
                    }
                }

            }
        }
        return result
    }

    private fun computeCascadeToWaypoint(state: CurrentState, cascadeTrigger: BpmnElementId, owner: BpmnElementId, type: PropertyType): Collection<CascadeTranslationToWaypoint> {
        return state.edges
                .filter { it.bpmnElement == owner }
                .map {
                    val index = if (type == SOURCE_REF) 0 else it.waypoint.size - 1
                    CascadeTranslationToWaypoint(cascadeTrigger, it.waypoint[index].id, it.id, it.waypoint[index].internalPhysicalPos)
                }
    }

    private fun drawBpmnEdges(shapes: List<EdgeWithIdentifiableWaypoints>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                drawWaypointElements(canvas, it, renderMeta).forEach { waypoint ->
                    mergeArea(waypoint.key, areaByElement, waypoint.value)
                }

                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    val bpmnRemoves = mutableListOf<BpmnElementRemovedEvent>()
                    val diagramRemoves = mutableListOf<DiagramElementRemovedEvent>()
                    it.bpmnElement?.apply { bpmnRemoves += BpmnElementRemovedEvent(this) }
                    diagramRemoves += DiagramElementRemovedEvent(it.id)
                    dest.addElementRemovedEvent(diagramRemoves, bpmnRemoves)
                }
                if (isDeepStructure(renderMeta)) {
                    val actionsElem = drawActionsElement(canvas, it, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback))
                    areaByElement += actionsElem
                }
            }
        }
    }

    private fun isDeepStructure(renderMeta: RenderMetadata): Boolean {
        return renderMeta.selectedIds.size == 1 && null == renderMeta.interactionContext.dragSelectionRect
    }

    private fun dramBpmnElements(shapes: List<ShapeElement>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    dest.addElementRemovedEvent(listOf(DiagramElementRemovedEvent(it.id)), listOf(BpmnElementRemovedEvent(it.bpmnElement)))
                }
                val newSequenceCallback = { dest: ProcessModelUpdateEvents ->
                    val elem = renderMeta.elementById[it.bpmnElement]
                    if (null != elem) {
                        val newSequenceBpmn = newElementsFactory().newOutgoingSequence(elem)
                        val newSequenceDiagram = newElementsFactory().newDiagramObject(EdgeElement::class, newSequenceBpmn)
                                .copy(waypoint = listOf(
                                        WaypointElement(it.bounds.x + it.bounds.width, it.bounds.y + it.bounds.height / 2.0f),
                                        WaypointElement(it.bounds.x + it.bounds.width + waypointLen, it.bounds.y + it.bounds.height / 2.0f)
                                ))
                        dest.addObjectEvent(BpmnEdgeObjectAddedEvent(newSequenceBpmn, EdgeElementState(newSequenceDiagram), newElementsFactory().propertiesOf(newSequenceBpmn)))
                    }
                }
                val isDeepStructure = isDeepStructure(renderMeta)
                if (isDeepStructure) {
                    val actionsElem = drawActionsElement(canvas, it, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback, Actions.NEW_LINK to newSequenceCallback))
                    areaByElement += actionsElem
                }

                renderMeta.interactionContext.dragEndCallbacks[it.id] = OnDragEnd@{ dx: Float, dy: Float, _: BpmnElementId? ->
                    val events = mutableListOf<DraggedToEvent>()
                    events += DraggedToEvent(it.id, dx, dy, null, null)
                    events += renderMeta
                            .cascadedTransalationOf
                            .filter { target -> target.cascadeSource == it.bpmnElement}
                            .filter { target -> !renderMeta.interactionContext.draggedIds.contains(target.waypointId) }
                            .map { cascadeTo -> DraggedToEvent(cascadeTo.waypointId, dx, dy, cascadeTo.parentEdgeId, cascadeTo.internalId) }
                    return@OnDragEnd events
                }
            }
        }
    }

    private fun drawAnchorsHit(canvas: CanvasPainter, anchors: AnchorHit) {
        anchors.anchors.forEach {
            when (it.key) {
                AnchorType.VERTICAL, AnchorType.HORIZONTAL -> canvas.drawZeroAreaLine(it.value, anchors.dragged, ANCHOR_STROKE, Colors.ANCHOR_COLOR.color)
                AnchorType.POINT -> canvas.drawCircle(it.value, anchorRadius, Colors.ANCHOR_COLOR.color)
            }
        }
    }

    private fun mergeArea(id: DiagramElementId, areas: MutableMap<DiagramElementId, AreaWithZindex>, area: AreaWithZindex) {
        val target = areas[id] ?: AreaWithZindex(Area(), area.dragCenter, area.areaType)
        target.area.add(area.area)
        target.anchorsForShape += area.anchorsForShape
        target.anchorsForWaypoints += area.anchorsForWaypoints
        areas[id] = AreaWithZindex(target.area, area.dragCenter, target.areaType, target.anchorsForWaypoints, target.anchorsForShape, min(target.index, area.index), target.parentToSelect ?: area.parentToSelect)
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): AreaWithZindex {
        val active = isActive(shape.id, meta)
        val area = Area()

        val color = color(active, Colors.ARROW_COLOR)
        val trueWaypoints = calculateTrueWaypoints(shape, meta)
        trueWaypoints.forEachIndexed { index, it ->
            when {
                index == trueWaypoints.size - 1 -> area.add(drawEdge(canvas, trueWaypoints[index - 1], it, meta, color, true))
                index > 0 -> area.add(drawEdge(canvas, trueWaypoints[index - 1], it, meta, color, false))
            }
        }

        return AreaWithZindex(area, Point2D.Float(0.0f, 0.0f), AreaType.POINT, if (active) mutableSetOf() else trueWaypoints.map { Point2D.Float(it.x, it.y) }.toMutableSet())
    }

    private fun drawWaypointElements(canvas: CanvasPainter, shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
        val area = HashMap<DiagramElementId, AreaWithZindex>()
        val trueWaypoints = calculateTrueWaypoints(shape, meta)
        // draw all endpoints only if none virtual is dragged and not physical
        val isVirtualDragged = meta.interactionContext.draggedIds.intersect(shape.waypoint.filter { !it.physical }.map { it.id }).isNotEmpty()
        val isPhysicalDragged = meta.interactionContext.draggedIds.intersect(shape.waypoint.filter { it.physical }.map { it.id }).isNotEmpty()
        val waypoints = if (!isDeepStructure(meta) || (isVirtualDragged || isPhysicalDragged)) trueWaypoints else shape.waypoint
        waypoints.forEachIndexed { index, it ->
            when {
                index == waypoints.size - 1 -> area.putAll(drawWaypointAnchors(canvas, waypoints[index - 1], it, shape, meta, true, index))
                index > 0 -> area.putAll(drawWaypointAnchors(canvas, waypoints[index - 1], it, shape, meta, false, index))
            }
        }

        return area
    }

    private fun drawWaypointAnchors(canvas: CanvasPainter, begin: IdentifiableWaypoint, end: IdentifiableWaypoint, parent: EdgeWithIdentifiableWaypoints, meta: RenderMetadata, isLast: Boolean, endWaypointIndex: Int): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()

        val dragCallback = DragCallback@{dx: Float, dy: Float, elem: IdentifiableWaypoint, droppedOn: BpmnElementId? ->
            val events = mutableListOf<Event>()
            val index = parent.waypoint.indexOf(elem)
            if (elem.physical) {
                events += DraggedToEvent(elem.id, dx, dy, parent.id, elem.internalPhysicalPos)
                if (null != droppedOn && null != parent.bpmnElement
                        && meta.interactionContext.draggedIds.containsAll(setOf(elem.id, parent.id))
                        && meta.interactionContext.draggedIds.size == 2) {
                    if (parent.waypoint.size - 1 == index ) {
                        events += StringValueUpdatedEvent(parent.bpmnElement!!, TARGET_REF, droppedOn.id)
                    } else if (0 == index) {
                        events += StringValueUpdatedEvent(parent.bpmnElement!!, SOURCE_REF, droppedOn.id)
                    }
                }
            } else {
                events += NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical || it.id == elem.id }
                                .map { if (it.id == elem.id && !it.physical) it.moveTo(dx, dy).asPhysical() else it }
                                .toList(),
                        parent.epoch + 1
                )
            }
            return@DragCallback events
        }

        val drawNode = { node: IdentifiableWaypoint, index: Int ->
            val translatedNode = translateElementIfNeeded(meta, node)
            val active = isActive(node.id, meta)
            val color = color(active, if (node.physical) Colors.WAYPOINT_COLOR else Colors.MID_WAYPOINT_COLOR)
            result[node.id] = AreaWithZindex(
                    canvas.drawCircle(translatedNode.asWaypointElement(), nodeRadius, color),
                    Point2D.Float(node.x, node.y),
                    AreaType.POINT,
                    mutableSetOf(Point2D.Float(translatedNode.x, translatedNode.y)),
                    mutableSetOf(),
                    ANCHOR_Z_INDEX,
                    parent.id
            )

            meta.interactionContext.dragEndCallbacks[node.id] = { dx: Float, dy: Float, droppedOn: BpmnElementId? -> dragCallback(dx, dy, node, droppedOn)}

            if (active && node.physical && index > 0 && (index < parent.waypoint.size - 1)) {
                val callback = { dest: ProcessModelUpdateEvents -> dest.addEvents(listOf(NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical }
                                .filter { it.id != node.id }
                                .toList(),
                        parent.epoch + 1
                )))}
                if (active && onlyWaypointAndEdgeSelected(meta, node, parent)) {
                    result += drawActionsElement(canvas, translatedNode, meta.interactionContext, mapOf(Actions.DELETE to callback))
                }
            }
        }

        if (isLast) {
            drawNode(begin, endWaypointIndex - 1)
            drawNode(end, endWaypointIndex)
            return result
        }

        drawNode(begin, endWaypointIndex - 1)
        meta.interactionContext.dragEndCallbacks[begin.id] = { dx: Float, dy: Float, droppedOn: BpmnElementId? -> dragCallback(dx, dy, begin, droppedOn)}
        return result
    }

    private fun onlyWaypointAndEdgeSelected(meta: RenderMetadata, node: IdentifiableWaypoint, parent: EdgeWithIdentifiableWaypoints) =
            meta.selectedIds.containsAll(setOf(node.id, parent.id)) && meta.selectedIds.size == 2

    private fun calculateTrueWaypoints(shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): List<IdentifiableWaypoint> {
        return shape.waypoint.filter { it.physical || isActive(it.id, meta) }
    }

    private fun drawEdge(canvas: CanvasPainter, begin: IdentifiableWaypoint, end: IdentifiableWaypoint, meta: RenderMetadata, color: Color, isLast: Boolean): Area {
        val translatedBegin = translateElementIfNeeded(meta, begin)
        val translatedEnd = translateElementIfNeeded(meta, end)
        if (isLast) {
            return canvas.drawLineWithArrow(translatedBegin.asWaypointElement(), translatedEnd.asWaypointElement(), color)
        }

        return canvas.drawLine(translatedBegin.asWaypointElement(), translatedEnd.asWaypointElement(), color)
    }

    private fun drawShapeElement(canvas: CanvasPainter, bpmnShape: ShapeElement, meta: RenderMetadata): AreaWithZindex {
        val elem = meta.elementById[bpmnShape.bpmnElement]
        val props = meta.elemPropertiesByElementId[bpmnShape.bpmnElement]
        val name = props?.get(NAME)?.value as String?
        val active = isActive(bpmnShape.id, meta)

        val shape = translateElementIfNeeded(meta, bpmnShape)

        return when (elem) {
            null -> defaultElementRender(canvas, bpmnShape, shape, name, active)
            is BpmnStartEvent -> drawStartEvent(canvas, bpmnShape, shape, active)
            is BpmnServiceTask -> drawServiceTask(canvas, bpmnShape, shape, name, active)
            is BpmnCallActivity -> drawCallActivity(canvas, bpmnShape, shape, name, active)
            is BpmnExclusiveGateway -> drawExclusiveGateway(canvas, bpmnShape, shape, active)
            is BpmnEndEvent -> drawEndEvent(canvas, bpmnShape, shape, active)
            else -> AreaWithZindex(Area(), Point2D.Float(0.0f, 0.0f), AreaType.SHAPE)
        }
    }

    private fun <T> translateElementIfNeeded(meta: RenderMetadata, elem: T): T where T : Translatable<T>, T: WithDiagramId {
        return if (canTranslate(meta, elem.id)) {
            elem.copyAndTranslate(
                    meta.interactionContext.current.x - meta.interactionContext.start.x,
                    meta.interactionContext.current.y - meta.interactionContext.start.y
            )
        } else {
            elem
        }
    }

    private fun canTranslate(meta: RenderMetadata, elemId: DiagramElementId) =
            meta.interactionContext.draggedIds.contains(elemId) || meta.cascadedTransalationOf.map { it.waypointId }.contains(elemId)

    private fun drawEndEvent(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawCircle(shape, color(active, Colors.END_EVENT), Colors.ELEMENT_BORDER_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawExclusiveGateway(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, EXCLUSIVE_GATEWAY, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawCallActivity(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawServiceTask(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIcon(shape, GEAR, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartEvent(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawCircle(shape, color(active, Colors.START_EVENT), Colors.ELEMENT_BORDER_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun defaultElementRender(canvas: CanvasPainter, originalShape: ShapeElement, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(originalShape.bounds.x, originalShape.bounds.y), AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun centerAnchor(shape: ShapeElement): MutableSet<Point2D.Float> {
        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy)
        )
    }

    private fun ellipseOrDiamondAnchors(shape: ShapeElement): MutableSet<Point2D.Float> {
        val halfWidth = shape.bounds.width / 2.0f
        val halfHeight = shape.bounds.height / 2.0f

        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy),

                Point2D.Float(cx - halfWidth, cy),
                Point2D.Float(cx + halfWidth, cy),
                Point2D.Float(cx, cy - halfHeight),
                Point2D.Float(cx, cy + halfHeight)
        )
    }

    private fun rectangleAnchors(shape: ShapeElement): MutableSet<Point2D.Float> {
        val halfWidth = shape.bounds.width / 2.0f
        val halfHeight = shape.bounds.height / 2.0f

        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
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

    private fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.SELECTED_COLOR.color else color.color
    }

    private fun isActive(elemId: DiagramElementId, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private fun drawActionsElement(canvas: CanvasPainter, edge: EdgeWithIdentifiableWaypoints, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        val minX = edge.waypoint.minBy { it.x }?.x ?: 0.0f
        val minY = edge.waypoint.minBy { it.y }?.y ?: 0.0f
        val maxX = edge.waypoint.maxBy { it.x }?.x ?: 0.0f
        val maxY = edge.waypoint.maxBy { it.y }?.y ?: 0.0f
        return drawActionsElement(
                canvas,
                edge.id,
                Point2D.Float(minX - activityToolBoxGap, minY - activityToolBoxGap),
                maxX - minX + activityToolBoxGap * 2.0f,
                maxY - minY + activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(canvas: CanvasPainter, waypoint: IdentifiableWaypoint, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        return drawActionsElement(
                canvas,
                waypoint.id,
                Point2D.Float(waypoint.x - activityToolBoxGap, waypoint.y - activityToolBoxGap),
                activityToolBoxGap * 2.0f,
                activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(canvas: CanvasPainter, shape: ShapeElement, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        return drawActionsElement(
                canvas,
                shape.id,
                Point2D.Float(shape.bounds.x - activityToolBoxGap, shape.bounds.y - activityToolBoxGap),
                shape.bounds.width + activityToolBoxGap * 2.0f,
                shape.bounds.height + activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(
            canvas: CanvasPainter,
            ownerId: DiagramElementId,
            location: Point2D.Float,
            width: Float,
            height: Float,
            ctx: ElementInteractionContext,
            actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>
    ): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()
        canvas.drawRectNoFill(location, width, height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        var yLocation = location.y
        actions.forEach {
            when(it.key) {
                Actions.DELETE -> {
                    val delId = DiagramElementId("DEL:$ownerId")
                    val deleteIconArea = canvas.drawIcon(BoundsElement(location.x + width + actionsMargin, yLocation, actionsIcoSize, actionsIcoSize), RECYCLE_BIN)
                    ctx.clickCallbacks[delId] = it.value
                    result[delId] = AreaWithZindex(deleteIconArea, Point2D.Float(0.0f, 0.0f), AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, ownerId)
                    yLocation += actionsIcoSize + actionsMargin
                }
                Actions.NEW_LINK -> {
                    val newLinkId = DiagramElementId("NEWLINK:$ownerId")
                    val newSequence = canvas.drawIcon(BoundsElement(location.x + width + actionsMargin, yLocation, actionsIcoSize, actionsIcoSize), SEQUENCE)
                    ctx.clickCallbacks[newLinkId] = it.value
                    result[newLinkId] = AreaWithZindex(newSequence, Point2D.Float(0.0f, 0.0f), AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, ownerId)
                    yLocation += actionsIcoSize + actionsMargin
                }
            }
        }

        return result
    }

    private data class RenderMetadata(
            val interactionContext: ElementInteractionContext,
            val selectedIds: Set<DiagramElementId>,
            val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
            val elementById: Map<BpmnElementId, WithBpmnId>,
            val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>,
            val cascadedTransalationOf: Set<CascadeTranslationToWaypoint>
    )

    private data class CascadeTranslationToWaypoint(val cascadeSource: BpmnElementId, val waypointId: DiagramElementId, val parentEdgeId: DiagramElementId, val internalId: Int)

    private enum class Actions {
        DELETE,
        NEW_LINK
    }

    fun String.asResource(): String? = BpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}