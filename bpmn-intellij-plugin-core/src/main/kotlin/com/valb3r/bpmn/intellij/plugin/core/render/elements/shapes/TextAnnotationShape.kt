package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.BasicStroke
import java.awt.Color

class TextAnnotationShape(
    elementId: DiagramElementId,
    bpmnElementId: BpmnElementId,
    shape: ShapeElement,
    state: () -> RenderState,
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override val areaType = AreaType.SHAPE

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {
        val area = ctx.canvas.drawRect(
            shapeCtx.shape,
            shapeCtx.name,
            Color.BLACK,
            Colors.INNER_TEXT_COLOR.color,
            BasicStroke(1.0f),
        )

        return mapOf(
            shapeCtx.diagramId to AreaWithZindex(
                area,
                AreaType.SHAPE,
                waypointAnchors(ctx.canvas.camera),
                shapeAnchors(ctx.canvas.camera),
                index = zIndex(),
                bpmnElementId = shape.bpmnElement,
            )
        )
    }
}
