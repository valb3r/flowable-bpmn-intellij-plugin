package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.geom.Ellipse2D

class EllipticIconOnLayerShape(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        private val icon: String,
        shape: ShapeElement,
        state: () -> RenderState,
        private val layerColor: Colors
) : ShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override val areaType: AreaType
        get() = AreaType.SHAPE

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawWrappedIconWithLayer(
                shapeCtx.shape,
                icon,
                isActive() || isTargetedByDrag(),
                selectionColor(),
                { Ellipse2D.Float(it.x, it.y, it.width, it.height) },
                layerColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = bpmnElementId))
    }
}