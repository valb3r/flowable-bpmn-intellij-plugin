package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.NAME
import java.awt.Color
import java.awt.geom.Area
import java.nio.charset.StandardCharsets

class BpmnProcessRenderer {

    val GEAR = IconLoader.getIcon("/icons/gear.png")
    val EXCLUSIVE_GATEWAY = "/icons/exclusive-gateway.svg".asResource()!!
    val DRAG = "/icons/drag.svg".asResource()!!

    fun render(ctx: RenderContext): Map<String, Area> {
        if (null == ctx.diagram) {
            return emptyMap()
        }

        val areaByElement: MutableMap<String, Area> = HashMap()
        val renderMeta = RenderMetadata(ctx.dragContext, ctx.selectedIds, ctx.diagram.elementById, ctx.diagram.elemPropertiesByElementId)

        dramBpmnElements(ctx.diagram, areaByElement, ctx.canvas, renderMeta)
        drawBpmnEdges(ctx.diagram, areaByElement, ctx.canvas, renderMeta)
        return areaByElement
    }

    private fun drawBpmnEdges(diagram: BpmnProcessObjectView, areaByElement: MutableMap<String, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        // TODO multi-diagram handling
        diagram
                .diagram[0]
                .bpmnPlane
                .bpmnEdge
                ?.forEach { mergeArea(it.bpmnElement ?: it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta)) }
    }

    private fun dramBpmnElements(diagram: BpmnProcessObjectView, areaByElement: MutableMap<String, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        // TODO multi-diagram handling
        diagram
                .diagram[0]
                .bpmnPlane
                .bpmnShape
                ?.forEach { mergeArea(it.bpmnElement, areaByElement, drawShapeElement(canvas, it, renderMeta)) }
    }

    private fun mergeArea(id: String, areas: MutableMap<String, Area>, area: Area) {
        val target = areas[id] ?: Area()
        target.add(area)
        areas[id] = target
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeElement, meta: RenderMetadata): Area {
        val elem = meta.elementById[shape.bpmnElement]
        val active = isActive(elem?.id, meta)
        val area = Area()
        shape.waypoint?.forEachIndexed { index, it ->
            when {
                index == shape.waypoint!!.size - 1 -> area.add(canvas.drawLineWithArrow(shape.waypoint!![index - 1], it, color(active, Colors.ARROW_COLOR)))
                index > 0 -> area.add(canvas.drawLine(shape.waypoint!![index - 1], it, color(active, Colors.ARROW_COLOR)))
            }
        }

        return area
    }

    private fun drawShapeElement(canvas: CanvasPainter, bpmnShape: ShapeElement, meta: RenderMetadata): Area {
        val elem = meta.elementById[bpmnShape.bpmnElement]
        val props = meta.elemPropertiesByElementId[bpmnShape.bpmnElement]
        val name = props?.get(NAME)?.value as String?
        val active = isActive(elem?.id, meta)

        val shape = if (meta.dragContext.draggedIds.contains(elem?.id)) {
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
            canvas.drawCircle(shape, name, color(active, Colors.RED), Colors.ELEMENT_BORDER_COLOR.color)

    private fun drawExclusiveGateway(canvas: CanvasPainter, shape: ShapeElement, active: Boolean) =
            canvas.drawWrappedIcon(shape, EXCLUSIVE_GATEWAY, active, Color.GREEN)

    private fun drawCallActivity(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)

    private fun drawServiceTask(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawRoundedRectWithIcon(shape, GEAR, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)

    private fun drawStartEvent(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean) =
            canvas.drawCircle(shape, name, color(active, Colors.GREEN), Colors.ELEMENT_BORDER_COLOR.color)

    private fun defaultElementRender(canvas: CanvasPainter, shape: ShapeElement, name: String?, active: Boolean): Area {
        return canvas.drawRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
    }

    private fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.GREEN.color else color.color
    }

    private fun isActive(elemId: String?, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private data class RenderMetadata(
            val dragContext: ElementDragContext,
            val selectedIds: Set<String>,
            val elementById: Map<String, WithId>,
            val elemPropertiesByElementId: Map<String, Map<PropertyType, Property>>
    )

    fun String.asResource(): String? = BpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}