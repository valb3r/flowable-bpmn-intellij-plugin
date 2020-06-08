package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Ellipse2D

class EllipticIconOnLayerShape(
        override val elementId: DiagramElementId,
        private val icon: String,
        shape: ShapeElement,
        state: CurrentState,
        private val layerColor: Colors
) : ShapeRenderElement(elementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawWrappedIconWithLayer(
                shapeCtx.shape,
                icon,
                isActive(),
                Colors.SELECTED_COLOR.color,
                { Ellipse2D.Float(it.x, it.y, it.width, it.height) },
                layerColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, AreaType.SHAPE, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera)))
    }
}