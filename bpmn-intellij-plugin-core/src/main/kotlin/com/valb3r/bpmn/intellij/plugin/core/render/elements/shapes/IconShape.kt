package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.geom.Point2D

open class IconShape(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        private val icon: String,
        shape: ShapeElement,
        state: RenderState
) : ShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawWrappedIcon(
                shapeCtx.shape,
                icon,
                isActive() || isTargetedByDrag(),
                selectionColor()
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType(), waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = bpmnElementId))
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Anchor(Point2D.Float(cx - halfWidth, cy)),
                Anchor(Point2D.Float(cx + halfWidth, cy)),
                Anchor(Point2D.Float(cx, cy - halfHeight)),
                Anchor(Point2D.Float(cx, cy + halfHeight))
        )
    }

    protected open fun areaType(): AreaType {
        return AreaType.SHAPE
    }
}