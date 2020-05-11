package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.NAME
import java.awt.Color
import java.awt.geom.Area
import java.nio.charset.StandardCharsets

class BpmnProcessRenderer {

    val nodeRadius = 2f

    val GEAR = IconLoader.getIcon("/icons/gear.png")
    val EXCLUSIVE_GATEWAY = "/icons/exclusive-gateway.svg".asResource()!!
    val DRAG = "/icons/drag.svg".asResource()!!

    fun render(ctx: RenderContext): Map<DiagramElementId, Area> {
        val state = ctx.stateProvider.currentState()
        val areaByElement: MutableMap<DiagramElementId, Area> = HashMap()
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

    private fun drawBpmnEdges(shapes: List<EdgeElement>, areaByElement: MutableMap<DiagramElementId, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach { mergeArea(it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta)) }
    }

    private fun dramBpmnElements(shapes: List<ShapeElement>, areaByElement: MutableMap<DiagramElementId, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach { mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, renderMeta)) }
    }

    private fun mergeArea(id: DiagramElementId, areas: MutableMap<DiagramElementId, Area>, area: Area) {
        val target = areas[id] ?: Area()
        target.add(area)
        areas[id] = target
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeElement, meta: RenderMetadata): Area {
        val active = isActive(shape.id, meta)
        val area = Area()
        if (null == shape.waypoint) {
            return area
        }

        val color = color(active, Colors.ARROW_COLOR)
        val drawFunction = if (active) {
            begin: WaypointElement, end: WaypointElement, isLast: Boolean -> drawEdgeWithAnchors(canvas, begin, end, color, isLast)
        } else {
            begin: WaypointElement, end: WaypointElement, isLast: Boolean -> drawEdgeWithoutAnchors(canvas, begin, end, color, isLast)
        }

        shape.waypoint?.forEachIndexed { index, it ->
            when {
                index == shape.waypoint!!.size - 1 -> area.add(drawFunction(shape.waypoint!![index - 1], it, true))
                index > 0 -> area.add(drawFunction(shape.waypoint!![index - 1], it, false))
            }
        }

        return area
    }

    private fun drawEdgeWithAnchors(canvas: CanvasPainter, begin: WaypointElement, end: WaypointElement, color: Color, isLast: Boolean): Area {
        val anchorColor = Color.RED
        val result = Area()
        if (isLast) {
            result.add(canvas.drawLineWithArrow(begin, end, color))
            result.add(canvas.drawCircle(begin, nodeRadius, anchorColor, anchorColor))
            result.add(canvas.drawCircle(end, nodeRadius, anchorColor, anchorColor))
            return result
        }

        result.add(canvas.drawLine(begin, end, color))
        result.add(canvas.drawCircle(begin, nodeRadius, anchorColor, anchorColor))
        result.add(canvas.drawCircle(end, nodeRadius, anchorColor, anchorColor))
        return result
    }

    private fun drawEdgeWithoutAnchors(canvas: CanvasPainter, begin: WaypointElement, end: WaypointElement, color: Color, isLast: Boolean): Area {
        if (isLast) {
            return canvas.drawLineWithArrow(begin, end, color)
        }

        return canvas.drawLine(begin, end, color)
    }

    private fun drawShapeElement(canvas: CanvasPainter, bpmnShape: ShapeElement, meta: RenderMetadata): Area {
        val elem = meta.elementById[bpmnShape.bpmnElement]
        val props = meta.elemPropertiesByElementId[bpmnShape.bpmnElement]
        val name = props?.get(NAME)?.value as String?
        val active = isActive(bpmnShape.id, meta)

        val shape = if (meta.dragContext.draggedIds.contains(bpmnShape.id)) {
            bpmnShape.copyAndTranslate(
                    meta.dragContext.current.x - meta.dragContext.start.x,
                    meta.dragContext.current.y - meta.dragContext.start.y
            )
        } else {
            bpmnShape
        }

        when (elem) {
            null -> return defaultElementRender(canvas, shape, name, active)
            is BpmnStartEvent -> return drawStartEvent(canvas, shape, name, active)
            is BpmnServiceTask -> return drawServiceTask(canvas, shape, name, active)
            is BpmnCallActivity -> return drawCallActivity(canvas, shape, name, active)
            is BpmnExclusiveGateway -> return drawExclusiveGateway(canvas, shape, active)
            is BpmnEndEvent -> return drawEndEvent(canvas, shape, name, active)
        }

        return Area()
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