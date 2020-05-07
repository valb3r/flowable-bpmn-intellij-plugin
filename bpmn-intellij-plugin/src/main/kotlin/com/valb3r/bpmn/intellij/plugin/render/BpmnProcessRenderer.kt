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

    fun render(canvas: CanvasPainter, selectedIds: Set<String>, diagram: BpmnProcessObjectView?): Map<String, Area> {
        if (null == diagram) {
            return emptyMap()
        }

        val areaByElement: MutableMap<String, Area> = HashMap()
        val renderMeta = RenderMetadata(selectedIds, diagram.elementById, diagram.elemPropertiesByElementId)

        dramBpmnElements(diagram, areaByElement, canvas, renderMeta)
        drawBpmnEdges(diagram, areaByElement, canvas, renderMeta)
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
                index == shape.waypoint!!.size - 1 -> area.add(canvas.drawGraphicsLineWithArrow(shape.waypoint!![index - 1], it, color(active, Colors.ARROW_COLOR)))
                index > 0 -> area.add(canvas.drawGraphicsLine(shape.waypoint!![index - 1], it, color(active, Colors.ARROW_COLOR)))
            }
        }

        return area
    }

    private fun drawShapeElement(canvas: CanvasPainter, shape: ShapeElement, meta: RenderMetadata): Area {
        val elem = meta.elementById[shape.bpmnElement]
        val props = meta.elemPropertiesByElementId[shape.bpmnElement]
        val name = props?.get(NAME)?.value as String?
        val active = isActive(elem?.id, meta)

        when (elem) {
            null -> return canvas.drawGraphicsRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
            is BpmnStartEvent -> return canvas.drawGraphicsRoundedRect(shape, name, color(active, Colors.GREEN), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
            is BpmnServiceTask -> return canvas.drawGraphicsRoundedRectWithIcon(shape, GEAR, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
            is BpmnCallActivity -> return canvas.drawGraphicsRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
            is BpmnExclusiveGateway -> return canvas.drawWrappedIcon(shape, EXCLUSIVE_GATEWAY, active, Color.GREEN)
            is BpmnEndEvent -> return canvas.drawGraphicsRoundedRect(shape, name, color(active, Colors.RED), Colors.ELEMENT_BORDER_COLOR.color, Colors.TEXT_COLOR.color)
        }

        return Area()
    }

    private fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.GREEN.color else color.color
    }

    private fun isActive(elemId: String?, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private data class RenderMetadata(
            val selectedIds: Set<String>,
            val elementById: Map<String, WithId>,
            val elemPropertiesByElementId: Map<String, Map<PropertyType, Property>>
    )

    fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}