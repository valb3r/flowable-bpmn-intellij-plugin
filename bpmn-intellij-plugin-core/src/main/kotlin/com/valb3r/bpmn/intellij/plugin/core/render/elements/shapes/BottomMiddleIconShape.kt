package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import javax.swing.Icon

class BottomMiddleIconShape(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        private val icon: Icon,
        shape: ShapeElement,
        state: () -> RenderState,
        private val backgroundColor: Colors = Colors.TRANSACTION_COLOR,
        private val borderColor: Colors =  Colors.TRANSACTION_ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR,
        override val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRectWithIconAtBottom(
                shapeCtx.shape,
                icon,
                shapeCtx.name,
                color(backgroundColor),
                borderColor.color,
                textColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), bpmnElementId = shape.bpmnElement, index = zIndex()))
    }
}