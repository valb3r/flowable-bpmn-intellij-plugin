package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Rectangle2D

class NoIconDoubleBorderShape(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        shape: ShapeElement,
        state: RenderState,
        private val backgroundColor: Colors = Colors.PROCESS_COLOR,
        private val borderColor: Colors =  Colors.ELEMENT_BORDER_COLOR,
        private val innerBorderColor: Colors =  Colors.TRANSACTION_ELEMENT_BORDER_COLOR,
        private val innerBackgroundColor: Colors = Colors.TRANSACTION_COLOR,
        private val textColor: Colors = Colors.SUBPROCESS_TEXT_COLOR,
        private val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    private val transactionalBoundaryMargin = 5.0f

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRect(
                shapeCtx.shape,
                null,
                color(backgroundColor),
                borderColor.color,
                textColor.color)

        ctx.canvas.drawRoundedRect(
                wrapInto(shapeCtx.shape, transactionalBoundaryMargin),
                shapeCtx.name,
                color(innerBackgroundColor),
                innerBorderColor.color,
                textColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = shape.bpmnElement))
    }

    private fun wrapInto(target: Rectangle2D.Float, margin: Float): Rectangle2D.Float {
        return Rectangle2D.Float(target.x + margin, target.y + margin, target.width - 2.0f * margin, target.height - 2.0f * margin)
    }
}