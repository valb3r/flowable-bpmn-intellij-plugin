package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import java.awt.Graphics2D
import java.awt.geom.Area

class BpmnProcessRenderer {

    fun render(canvas: CanvasPainter, selectedIds: Set<String>, diagram: BpmnProcessObjectView?): Map<String, Area> {
        if (null == diagram) {
            return emptyMap()
        }

        val areaByElement: MutableMap<String, Area> = HashMap()
        val renderMeta = RenderMetadata(selectedIds, diagram.elementById)

        dramBpmnElements(diagram, areaByElement, canvas, renderMeta)
        drawBpmnEdges(diagram, areaByElement, canvas, renderMeta)
        return areaByElement
    }

    private fun drawBpmnEdges(diagram: BpmnProcessObjectView, areaByElement: MutableMap<String, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        diagram
                .diagram
                .bpmnPlane
                .bpmnEdge
                ?.forEach { mergeArea(it.bpmnElement ?: it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta)) }
    }

    private fun dramBpmnElements(diagram: BpmnProcessObjectView, areaByElement: MutableMap<String, Area>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        diagram
                .diagram
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
                index == shape.waypoint!!.size - 1 -> area.add(canvas.drawGraphicsLineWithArrow(shape.waypoint!![index - 1], it, color(active, Colors.UN_HIGHLIGHTED_COLOR)))
                index > 0 -> area.add(canvas.drawGraphicsLine(shape.waypoint!![index - 1], it, color(active, Colors.UN_HIGHLIGHTED_COLOR)))
            }
        }

        return area
    }

    private fun drawShapeElement(canvas: CanvasPainter, shape: ShapeElement, meta: RenderMetadata): Area {
        val elem = meta.elementById[shape.bpmnElement]
        val active = isActive(elem?.id, meta)

        when (elem) {
            null -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.NEUTRAL_COLOR))
            is BpmnStartEvent -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.GREEN))
            is BpmnServiceTask -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.UN_HIGHLIGHTED_COLOR))
            is BpmnCallActivity -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.DOWNSTREAM_COLOR))
            is BpmnExclusiveGateway -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.YELLOW))
            is BpmnEndEvent -> return canvas.drawGraphicsRoundedRect(shape,  color(active, Colors.RED))
        }

        return Area()
    }

    private fun drawCallActivity(graphics: Graphics2D) {
    }

    private fun drawExclusiveGateway(graphics: Graphics2D) {
    }

    private fun color(active: Boolean, color: Colors): Colors {
        return if (active) Colors.GREEN else color
    }

    private fun isActive(elemId: String?, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private data class RenderMetadata(
            val selectedIds: Set<String>,
            val elementById: Map<String, WithId>
    )
}