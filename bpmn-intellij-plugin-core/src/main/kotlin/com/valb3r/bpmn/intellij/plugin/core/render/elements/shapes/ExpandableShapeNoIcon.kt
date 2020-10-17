package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.properties.uionly.UiOnlyPropertyType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.buttons.ButtonWithAnchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.ExpandViewTransform
import java.awt.geom.Point2D
import javax.swing.Icon

class ExpandableShapeNoIcon(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        private val collapsed: Boolean,
        plusIcon: Icon,
        minusIcon: Icon,
        shape: ShapeElement,
        state: RenderState,
        private val backgroundColor: Colors = Colors.TRANSACTION_COLOR,
        private val borderColor: Colors =  Colors.TRANSACTION_ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR,
        private val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    private val expandButton = ButtonWithAnchor(
            DiagramElementId("EXPAND:" + shape.id.id),
            Point2D.Float((shape.bounds().first.x + shape.bounds().second.x) / 2.0f, shape.bounds().second.y),
            if (collapsed) plusIcon else minusIcon,
            { mutableListOf(BooleanUiOnlyValueUpdatedEvent(bpmnElementId, UiOnlyPropertyType.EXPANDED, !collapsed)) },
            state
    )

    override val children: MutableList<BaseDiagramRenderElement> = (
            super.children + expandButton

    ).toMutableList()

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

    override fun createIfNeededExpandViewTransform() {
        if (this.collapsed) {
            return
        }

        state.baseTransform.addPreTransform(ExpandViewTransform(
                elementId,
                shape.rectBounds(),
                shape.rectBounds().centerX.toFloat(),
                shape.rectBounds().centerY.toFloat(),
                100.0f,
                100.0f,
                enumerateChildrenRecursively().map { it.elementId }.toSet()
        ))
        super.createIfNeededExpandViewTransform()
    }
}