package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.geom.Point2D
import javax.swing.Icon

abstract class IconAnchorElement(
        elementId: DiagramElementId,
        attachedTo: DiagramElementId?,
        currentLocation: Point2D.Float,
        state: () -> RenderState
): AnchorElement(elementId, attachedTo, currentLocation, state) {

    override val areaType: AreaType
        get() = AreaType.SHAPE

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(Anchor(Point2D.Float(cx, cy)))
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val icon = icon()
        val active = isActive()
        val rect = currentOnScreenRect(ctx.canvas.camera)
        val area = ctx.canvas.drawIcon(Point2D.Float(rect.x, rect.y), icon, if (active) Colors.SELECTED_COLOR.color else null)

        state().ctx.interactionContext.dragEndCallbacks[elementId] = {
            dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOn: Map<BpmnElementId, AreaWithZindex> -> onDragEnd(dx, dy, droppedOn, allDroppedOn)
        }

        return mutableMapOf(elementId to AreaWithZindex(area, areaType, index = zIndex(), anchorsForShape = shapeAnchors(ctx.canvas.camera)))
    }

    protected abstract fun icon(): Icon
}