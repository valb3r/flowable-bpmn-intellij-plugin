package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.NAME
import com.valb3r.bpmn.intellij.plugin.events.*
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.nio.charset.StandardCharsets
import kotlin.math.min

class BpmnProcessRenderer {

    private val waypointLen = 40.0f
    private val activityToolBoxGap = 5.0f
    private val nodeRadius = 3f
    private val actionsIcoSize = 15f
    private val actionsMargin = 5f

    private val GEAR = IconLoader.getIcon("/icons/gear.png")
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
                state.elementByStaticId,
                state.elemPropertiesByStaticElementId
        )

        drawAnchorsHit(ctx.canvas, ctx.interactionContext.anchorsHit)
        dramBpmnElements(state.shapes, areaByElement, ctx.canvas, renderMeta)
        drawBpmnEdges(state.edges, areaByElement, ctx.canvas, renderMeta)

        return areaByElement
    }

    private fun drawBpmnEdges(shapes: List<EdgeWithIdentifiableWaypoints>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                drawWaypointElements(canvas, it, renderMeta).forEach {waypoint ->
                    mergeArea(waypoint.key, areaByElement, waypoint.value)
                }

                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    it.bpmnElement?.apply { dest.addElementRemovedEvent(BpmnElementRemovedEvent(this)) }
                    dest.addElementRemovedEvent(DiagramElementRemovedEvent(it.id))
                }
                val actionsElem = drawActionsElement(canvas, it, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback))
                areaByElement += actionsElem
            }
        }
    }

    private fun dramBpmnElements(shapes: List<ShapeElement>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    dest.addElementRemovedEvent(DiagramElementRemovedEvent(it.id))
                    dest.addElementRemovedEvent(BpmnElementRemovedEvent(it.bpmnElement))
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
                val actionsElem = drawActionsElement(canvas, it, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback, Actions.NEW_LINK to newSequenceCallback))
                areaByElement += actionsElem
                renderMeta.interactionContext.dragEndCallbacks[it.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents, droppedOn: BpmnElementId? -> dest.addLocationUpdateEvent(DraggedToEvent(it.id, dx, dy, null, null))}
            }
        }
    }

    private fun drawAnchorsHit(canvas: CanvasPainter, anchors: Set<Pair<Point2D.Float, Point2D.Float>>) {
        anchors.forEach {
            canvas.drawZeroAreaLine(it.first, it.second, ANCHOR_STROKE, Colors.ANCHOR_COLOR.color)
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
        val waypoints = if (isVirtualDragged || isPhysicalDragged) trueWaypoints else shape.waypoint
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

        val dragCallback = {dx: Float, dy: Float, dest: ProcessModelUpdateEvents, elem: IdentifiableWaypoint, droppedOn: BpmnElementId? ->
            val index = parent.waypoint.indexOf(elem)
            if (elem.physical) {
                dest.addLocationUpdateEvent(DraggedToEvent(elem.id, dx, dy, parent.id, elem.internalPhysicalPos))
                if (null != droppedOn && null != parent.bpmnElement) {
                    if (parent.waypoint.size - 1 == index ) {
                        dest.addPropertyUpdateEvent(StringValueUpdatedEvent(parent.bpmnElement!!, PropertyType.TARGET_REF, droppedOn.id))
                    } else if (0 == index) {
                        dest.addPropertyUpdateEvent(StringValueUpdatedEvent(parent.bpmnElement!!, PropertyType.SOURCE_REF, droppedOn.id))
                    }
                }
            } else {
                dest.addWaypointStructureUpdate(NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical || it.id == elem.id }
                                .map { if (it.id == elem.id && !it.physical) it.moveTo(dx, dy).asPhysical() else it }
                                .toList(),
                        parent.epoch + 1
                ))
            }
        }

        val drawNode = { node: IdentifiableWaypoint, index: Int ->
            val translatedNode = translateElement(meta, node)
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
            meta.interactionContext.dragEndCallbacks[node.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents, droppedOn: BpmnElementId? -> dragCallback(dx, dy, dest, node, droppedOn)}
            if (active && node.physical && index > 0 && (index < parent.waypoint.size - 1)) {
                val callback = { dest: ProcessModelUpdateEvents -> dest.addWaypointStructureUpdate(NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical }
                                .filter { it.id != node.id }
                                .toList(),
                        parent.epoch + 1
                ))}
                result += drawActionsElement(canvas, translatedNode, meta.interactionContext, mapOf(Actions.DELETE to callback))
            }
        }

        if (isLast) {
            drawNode(begin, endWaypointIndex - 1)
            drawNode(end, endWaypointIndex)
            return result
        }

        drawNode(begin, endWaypointIndex - 1)
        meta.interactionContext.dragEndCallbacks[begin.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents, droppedOn: BpmnElementId? -> dragCallback(dx, dy, dest, begin, droppedOn)}
        return result
    }

    private fun calculateTrueWaypoints(shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): List<IdentifiableWaypoint> {
        return shape.waypoint.filter { it.physical || isActive(it.id, meta) }
    }

    private fun drawEdge(canvas: CanvasPainter, begin: IdentifiableWaypoint, end: IdentifiableWaypoint, meta: RenderMetadata, color: Color, isLast: Boolean): Area {
        val translatedBegin = translateElement(meta, begin)
        val translatedEnd = translateElement(meta, end)
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

        val shape = translateElement(meta, bpmnShape)

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

    private fun <T> translateElement(meta: RenderMetadata, elem: T): T where T : Translatable<T>, T: WithDiagramId {
        return if (meta.interactionContext.draggedIds.contains(elem.id)) {
            elem.copyAndTranslate(
                    meta.interactionContext.current.x - meta.interactionContext.start.x,
                    meta.interactionContext.current.y - meta.interactionContext.start.y
            )
        } else {
            elem
        }
    }

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
            val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>
    )

    private enum class Actions {
        DELETE,
        NEW_LINK
    }

    fun String.asResource(): String? = BpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}