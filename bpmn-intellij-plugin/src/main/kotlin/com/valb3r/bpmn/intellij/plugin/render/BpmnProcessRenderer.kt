package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.NAME
import com.valb3r.bpmn.intellij.plugin.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.events.NewWaypointsEvent
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.nio.charset.StandardCharsets
import kotlin.math.min

class BpmnProcessRenderer {

    private val nodeRadius = 3f
    private val recycleBinSize = 20f
    private val recycleBinMargin = 10f

    private val GEAR = IconLoader.getIcon("/icons/gear.png")
    private val EXCLUSIVE_GATEWAY = "/icons/exclusive-gateway.svg".asResource()!!
    private val RECYCLE_BIN = "/icons/recycle-bin.svg".asResource()!!

    private val ANCHOR_STROKE = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(5.0f), 0.0f)

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

    private fun drawBpmnEdges(shapes: List<EdgeElementState>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                drawWaypointElements(canvas, it, renderMeta).forEach {waypoint ->
                    mergeArea(waypoint.key, areaByElement, waypoint.value)
                }
            }
        }
    }

    private fun dramBpmnElements(shapes: List<ShapeElement>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, renderMeta))
            renderMeta.interactionContext.dragEndCallbacks[it.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents -> dest.addLocationUpdateEvent(DraggedToEvent(it.id, dx, dy))}
        }
    }

    private fun drawAnchorsHit(canvas: CanvasPainter, anchors: Set<Pair<Point2D.Float, Point2D.Float>>) {
        anchors.forEach {
            canvas.drawZeroAreaLine(it.first, it.second, ANCHOR_STROKE, Colors.ANCHOR_COLOR.color)
        }
    }

    private fun mergeArea(id: DiagramElementId, areas: MutableMap<DiagramElementId, AreaWithZindex>, area: AreaWithZindex) {
        val target = areas[id] ?: AreaWithZindex(Area(), area.dragCenter)
        target.area.add(area.area)
        target.anchors += area.anchors
        areas[id] = AreaWithZindex(target.area, area.dragCenter, target.anchors, min(target.index, area.index), target.parentToSelect ?: area.parentToSelect)
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeElementState, meta: RenderMetadata): AreaWithZindex {
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

        return AreaWithZindex(area, Point2D.Float(0.0f, 0.0f), if (active) mutableSetOf() else trueWaypoints.map { Point2D.Float(it.x, it.y) }.toMutableSet())
    }

    private fun drawWaypointElements(canvas: CanvasPainter, shape: EdgeElementState, meta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
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

    private fun drawWaypointAnchors(canvas: CanvasPainter, begin: WaypointElementState, end: WaypointElementState, parent: EdgeElementState, meta: RenderMetadata, isLast: Boolean, endWaypointIndex: Int): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()

        val dragCallback = {dx: Float, dy: Float, dest: ProcessModelUpdateEvents, elem: WaypointElementState ->
            if (elem.physical) {
                dest.addLocationUpdateEvent(DraggedToEvent(elem.id, dx, dy))
            } else {
                dest.addWaypointStructureUpdate(NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical || it.id == elem.id }
                                .map { if (it.id == elem.id && !it.physical) it.moveTo(dx, dy).asPhysical() else it.originalLocation() }
                                .toList()
                ))
            }
        }

        val drawNode = { node: WaypointElementState, index: Int ->
            val translatedNode = translateElement(meta, node)
            val active = isActive(node.id, meta)
            val color = color(active, if (node.physical) Colors.WAYPOINT_COLOR else Colors.MID_WAYPOINT_COLOR)
            result[node.id] = AreaWithZindex(
                    canvas.drawCircle(translatedNode.asWaypointElement(), nodeRadius, color),
                    Point2D.Float(node.x, node.y),
                    mutableSetOf(Point2D.Float(translatedNode.x, translatedNode.y)),
                    ANCHOR_Z_INDEX, parent.id
            )
            meta.interactionContext.dragEndCallbacks[node.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents -> dragCallback(dx, dy, dest, node)}
            if (active && node.physical && index > 0 && (index < parent.waypoint.size - 1)) {
                val delId = DiagramElementId("DEL:" + node.id)
                val deleteIconArea = canvas.drawIcon(BoundsElement(translatedNode.x + recycleBinMargin, translatedNode.y + recycleBinMargin, recycleBinSize, recycleBinSize), RECYCLE_BIN)
                meta.interactionContext.clickCallbacks[delId] = { dest: ProcessModelUpdateEvents -> dest.addWaypointStructureUpdate(NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical }
                                .filter { it.id != node.id }
                                .map { it.originalLocation() }
                                .toList()
                ))}
                result[delId] = AreaWithZindex(deleteIconArea, Point2D.Float(node.x, node.y), mutableSetOf(), ANCHOR_Z_INDEX, parent.id)
            }
        }

        if (isLast) {
            drawNode(begin, endWaypointIndex - 1)
            drawNode(end, endWaypointIndex)
            return result
        }

        drawNode(begin, endWaypointIndex - 1)
        meta.interactionContext.dragEndCallbacks[begin.id] = { dx: Float, dy: Float, dest: ProcessModelUpdateEvents -> dragCallback(dx, dy, dest, begin)}
        return result
    }

    private fun calculateTrueWaypoints(shape: EdgeElementState, meta: RenderMetadata): List<WaypointElementState> {
        return shape.waypoint.filter { it.physical || isActive(it.id, meta) }
    }

    private fun drawEdge(canvas: CanvasPainter, begin: WaypointElementState, end: WaypointElementState, meta: RenderMetadata, color: Color, isLast: Boolean): Area {
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
            null -> defaultElementRender(canvas, shape, name, active)
            is BpmnStartEvent -> drawStartEvent(canvas, shape, active)
            is BpmnServiceTask -> drawServiceTask(canvas, shape, name, active)
            is BpmnCallActivity -> drawCallActivity(canvas, shape, name, active)
            is BpmnExclusiveGateway -> drawExclusiveGateway(canvas, shape, active)
            is BpmnEndEvent -> drawEndEvent(canvas, shape, active)
            else -> AreaWithZindex(Area(), Point2D.Float(0.0f, 0.0f))
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

    private fun drawEndEvent(canvas: CanvasPainter, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawCircle(shape, color(active, Colors.RED), Colors.ELEMENT_BORDER_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), ellipseOrDiamondAnchors(shape))
    }

    private fun drawExclusiveGateway(canvas: CanvasPainter, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, EXCLUSIVE_GATEWAY, active, Color.GREEN)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), ellipseOrDiamondAnchors(shape))
    }

    private fun drawCallActivity(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), rectangleAnchors(shape))
    }

    private fun drawServiceTask(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIcon(shape, GEAR, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), rectangleAnchors(shape))
    }

    private fun drawStartEvent(canvas: CanvasPainter, shape: ShapeElement, active: Boolean): AreaWithZindex {
        val area = canvas.drawCircle(shape, color(active, Colors.GREEN), Colors.ELEMENT_BORDER_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), ellipseOrDiamondAnchors(shape))
    }

    private fun defaultElementRender(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
        return AreaWithZindex(area, Point2D.Float(shape.bounds.x, shape.bounds.y), rectangleAnchors(shape))
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
        return if (active) Colors.GREEN.color else color.color
    }

    private fun isActive(elemId: DiagramElementId, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private data class RenderMetadata(
            val interactionContext: ElementInteractionContext,
            val selectedIds: Set<DiagramElementId>,
            val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
            val elementById: Map<BpmnElementId, WithId>,
            val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>
    )

    fun String.asResource(): String? = BpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}