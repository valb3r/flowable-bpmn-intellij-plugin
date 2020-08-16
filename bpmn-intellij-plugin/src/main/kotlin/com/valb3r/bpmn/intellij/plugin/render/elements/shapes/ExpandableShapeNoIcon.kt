package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.buttons.ButtonWithAnchor
import java.awt.geom.Point2D
import javax.swing.Icon

class ExpandableShapeNoIcon(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        private val plusIcon: Icon,
        private val minusIcon: Icon,
        shape: ShapeElement,
        state: RenderState,
        private val backgroundColor: Colors = Colors.TRANSACTION_COLOR,
        private val borderColor: Colors =  Colors.TRANSACTION_ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR,
        private val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override val children: MutableList<BaseDiagramRenderElement> = mutableListOf(
            ButtonWithAnchor(DiagramElementId("EXPAND:" + shape.id.id), Point2D.Float((shape.bounds().first.x + shape.bounds().second.x) / 2.0f, shape.bounds().second.y), plusIcon, { mutableListOf() }, state),
            edgeExtractionAnchor
    )

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRect(
                shapeCtx.shape,
                shapeCtx.name,
                color(backgroundColor),
                borderColor.color,
                textColor.color
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = bpmnElementId))
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
}