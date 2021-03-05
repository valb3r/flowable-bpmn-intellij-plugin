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

class TopLeftIconShape(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        private val icon: Icon,
        shape: ShapeElement,
        state: RenderState,
        private val backgroundColor: Colors = Colors.SERVICE_TASK_COLOR,
        private val borderColor: Colors =  Colors.ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override val areaType: AreaType
        get() = AreaType.SHAPE

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRectWithIconAtCorner(
                shapeCtx.shape,
                icon,
                shapeCtx.name,
                color(backgroundColor),
                borderColor.color,
                textColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = bpmnElementId))
    }
}