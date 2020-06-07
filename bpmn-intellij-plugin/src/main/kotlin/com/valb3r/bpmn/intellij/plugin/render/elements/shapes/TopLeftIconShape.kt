package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.State
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import javax.swing.Icon

class TopLeftIconShape(
        private val icon: Icon,
        shape: ShapeElement,
        elemState: State,
        state: CurrentState,
        parent: BaseRenderElement,
        childrenElems: Set<BaseRenderElement>
) : ShapeRenderElement(shape, elemState, state, parent, childrenElems) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRectWithIconAtCorner(
                shapeCtx.shape,
                icon,
                shapeCtx.name,
                color(isActive(), Colors.SERVICE_TASK_COLOR),
                Colors.ELEMENT_BORDER_COLOR.color,
                Colors.INNER_TEXT_COLOR.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, AreaType.SHAPE, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera)))
    }
}