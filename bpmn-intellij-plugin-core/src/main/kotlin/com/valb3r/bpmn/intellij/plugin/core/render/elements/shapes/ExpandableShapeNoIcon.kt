package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.properties.uionly.UiOnlyPropertyType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.buttons.ButtonWithAnchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.ExpandViewTransform
import java.awt.geom.Point2D
import javax.swing.Icon

class ExpandableShapeNoIcon(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        plusIcon: Icon,
        minusIcon: Icon,
        shape: ShapeElement,
        state: () -> RenderState,
        private val backgroundColor: Colors = Colors.TRANSACTION_COLOR,
        private val borderColor: Colors =  Colors.TRANSACTION_ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR,
        override val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override fun addInnerElement(elem: BaseDiagramRenderElement) {
        elem.viewTransformLevel = this.elementId
        innerElements.add(elem)
    }

    private val expandButton = ButtonWithAnchor(
            DiagramElementId("EXPAND:" + shape.id.id),
            Point2D.Float((shape.bounds().first.x + shape.bounds().second.x) / 2.0f, shape.bounds().second.y),
            if (isCollapsed()) plusIcon else minusIcon,
            { mutableListOf(BooleanUiOnlyValueUpdatedEvent(bpmnElementId, UiOnlyPropertyType.EXPANDED, isCollapsed())) },
            state
    )

    override val children: List<BaseDiagramRenderElement>
        get() = ((if (!isCollapsed()) innerElements else mutableListOf()) + actions + expandButton)

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
        if (isCollapsed()) {
            return
        }

        state().baseTransform.addPreTransform(ExpandViewTransform(
                elementId,
                viewTransformLevel!!,
                shape.rectBounds(),
                shape.rectBounds().centerX.toFloat(),
                shape.rectBounds().centerY.toFloat(),
                100.0f,
                100.0f,
        ))
        super.createIfNeededExpandViewTransform()
    }

    // Simplifying anchor model as inversion of view transform of intermediate anchors is not stable
    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
            Anchor(Point2D.Float(cx - halfWidth, cy), 10),
            Anchor(Point2D.Float(cx + halfWidth, cy), 10),
            Anchor(Point2D.Float(cx, cy - halfHeight), 10),
            Anchor(Point2D.Float(cx, cy + halfHeight), 10),
        )
    }

    // Central anchor does not make sense for this kind of shape
    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    private fun isCollapsed(): Boolean {
        return !(state().currentState.elemUiOnlyPropertiesByStaticElementId[bpmnElementId]?.get(UiOnlyPropertyType.EXPANDED)?.value as Boolean? ?: false)
    }
}
