package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState

class IconShape(
        override val elementId: DiagramElementId,
        private val icon: String,
        shape: ShapeElement,
        state: RenderState
) : ShapeRenderElement(elementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawWrappedIcon(
                shapeCtx.shape,
                icon,
                isActive(),
                Colors.SELECTED_COLOR.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, AreaType.SHAPE, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera)))
    }
}