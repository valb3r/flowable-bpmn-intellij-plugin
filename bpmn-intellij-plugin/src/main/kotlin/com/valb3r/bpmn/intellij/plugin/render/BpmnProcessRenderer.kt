package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.NAME
import com.valb3r.bpmn.intellij.plugin.render.dto.EdgeElementState
import com.valb3r.bpmn.intellij.plugin.render.dto.WaypointElementState
import java.awt.Color
import java.awt.geom.Area
import java.nio.charset.StandardCharsets
import kotlin.math.min

class BpmnProcessRenderer {

    private val nodeRadius = 3f

    val GEAR = IconLoader.getIcon("/icons/gear.png")
    val EXCLUSIVE_GATEWAY = "/icons/exclusive-gateway.svg".asResource()!!
    val DRAG = "/icons/drag.svg".asResource()!!

    fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val state = ctx.stateProvider.currentState()
        val areaByElement: MutableMap<DiagramElementId, AreaWithZindex> = HashMap()
        val renderMeta = RenderMetadata(
                ctx.dragContext,
                ctx.selectedIds,
                state.elementByDiagramId,
                state.elementByStaticId,
                state.elemPropertiesByStaticElementId
        )

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
        shapes.forEach { mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, renderMeta)) }
    }

    private fun mergeArea(id: DiagramElementId, areas: MutableMap<DiagramElementId, AreaWithZindex>, area: AreaWithZindex) {
        val target = areas[id] ?: AreaWithZindex(Area())
        target.area.add(area.area)
        areas[id] = AreaWithZindex(target.area, min(target.index, area.index), target.parentToSelect ?: area.parentToSelect)
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeElementState, meta: RenderMetadata): AreaWithZindex {
        val active = isActive(shape.id, meta)
        val area = Area()

        val color = color(active, Colors.ARROW_COLOR)
        shape.waypoint.forEachIndexed { index, it ->
            when {
                index == shape.waypoint.size - 1 -> area.add(drawEdge(canvas, shape.waypoint[index - 1], it, meta, color, true))
                index > 0 -> area.add(drawEdge(canvas, shape.waypoint[index - 1], it, meta, color, false))
            }
        }

        return AreaWithZindex(area)
    }

    private fun drawWaypointElements(canvas: CanvasPainter, shape: EdgeElementState, meta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
        val area = HashMap<DiagramElementId, AreaWithZindex>()
        shape.waypoint.forEachIndexed { index, it ->
            when {
                index == shape.waypoint.size - 1 -> area.putAll(drawWaypointAnchors(canvas, shape.waypoint[index - 1], it, shape.id, meta, true))
                index > 0 -> area.putAll(drawWaypointAnchors(canvas, shape.waypoint[index - 1], it, shape.id, meta, false))
            }
        }

        return area
    }

    private fun drawWaypointAnchors(canvas: CanvasPainter, begin: WaypointElementState, end: WaypointElementState, parent: DiagramElementId, meta: RenderMetadata, isLast: Boolean): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()
        val translatedBegin = translateElement(meta, begin)
        val translatedEnd = translateElement(meta, end)
        if (isLast) {
            result[begin.id] = AreaWithZindex(canvas.drawCircle(translatedBegin.asWaypointElement(), nodeRadius, color(isActive(begin.id, meta), Colors.WAYPOINT_COLOR)), ANCHOR_Z_INDEX, parent)
            result[end.id] = AreaWithZindex(canvas.drawCircle(translatedEnd.asWaypointElement(), nodeRadius, color(isActive(end.id, meta), Colors.WAYPOINT_COLOR)), ANCHOR_Z_INDEX, parent)
            return result
        }

        result[begin.id] = AreaWithZindex(canvas.drawCircle(translatedBegin.asWaypointElement(), nodeRadius, color(isActive(begin.id, meta), Colors.WAYPOINT_COLOR)), ANCHOR_Z_INDEX, parent)
        return result
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

        val resultArea = when (elem) {
            null -> defaultElementRender(canvas, shape, name, active)
            is BpmnStartEvent -> drawStartEvent(canvas, shape, name, active)
            is BpmnServiceTask -> drawServiceTask(canvas, shape, name, active)
            is BpmnCallActivity -> drawCallActivity(canvas, shape, name, active)
            is BpmnExclusiveGateway -> drawExclusiveGateway(canvas, shape, active)
            is BpmnEndEvent -> drawEndEvent(canvas, shape, name, active)
            else -> Area()
        }

        return AreaWithZindex(resultArea)
    }

    private fun <T> translateElement(meta: RenderMetadata, elem: T): T where T : Translatable<T>, T: WithDiagramId {
        return if (meta.dragContext.draggedIds.contains(elem.id)) {
            elem.copyAndTranslate(
                    meta.dragContext.current.x - meta.dragContext.start.x,
                    meta.dragContext.current.y - meta.dragContext.start.y
            )
        } else {
            elem
        }
    }

    private fun drawEndEvent(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawCircle(shape, color(active, Colors.RED), Colors.ELEMENT_BORDER_COLOR.color)

    private fun drawExclusiveGateway(canvas: CanvasPainter, shape: ShapeElement, active: Boolean) =
            canvas.drawWrappedIcon(shape, EXCLUSIVE_GATEWAY, active, Color.GREEN)

    private fun drawCallActivity(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)

    private fun drawServiceTask(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawRoundedRectWithIcon(shape, GEAR, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)

    private fun drawStartEvent(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawCircle(shape, color(active, Colors.GREEN), Colors.ELEMENT_BORDER_COLOR.color)

    private fun defaultElementRender(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean): Area {
        return canvas.drawRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
    }

    private fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.GREEN.color else color.color
    }

    private fun isActive(elemId: DiagramElementId, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private data class RenderMetadata(
            val dragContext: ElementDragContext,
            val selectedIds: Set<DiagramElementId>,
            val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
            val elementById: Map<BpmnElementId, WithId>,
            val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>
    )

    fun String.asResource(): String? = BpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}