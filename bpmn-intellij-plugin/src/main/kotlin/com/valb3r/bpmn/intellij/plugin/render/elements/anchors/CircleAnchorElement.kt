package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class CircleAnchorElement(
        override val elementId: DiagramElementId,
        currentLocation: Point2D.Float,
        private val radius: Float,
        private val bodyColor: Colors,
        state: RenderState
) : AnchorElement(elementId, currentLocation, state) {

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        return viewTransform.transform(
                Rectangle2D.Float(
                        location.x - radius,
                        location.y - radius,
                        2.0f * radius,
                        2.0f * radius
                )
        )
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        if (!isRenderable()) {
            return mutableMapOf()
        }

        val rect = currentOnScreenRect(ctx.canvas.camera)
        if (!isVisible() || ifVisibleNoRenderIf()) {
            val point = ctx.canvas.camera.toCameraView(Point2D.Float(rect.x, rect.y))
            return mutableMapOf(
                    elementId to AreaWithZindex(Area(Rectangle2D.Float(point.x, point.y, 0.1f, 0.1f)), AreaType.POINT, waypointAnchors(ctx.canvas.camera), index = zIndex())
            )
        }

        val currentColor = color(bodyColor)
        val area = ctx.canvas.drawEllipse(
                rect,
                currentColor,
                currentColor
        )

        state.ctx.interactionContext.dragEndCallbacks[elementId] = {
            dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOn: Map<BpmnElementId, AreaWithZindex> -> onDragEnd(dx, dy, droppedOn, allDroppedOn)
        }
        return mutableMapOf(elementId to AreaWithZindex(area, AreaType.POINT, waypointAnchors(ctx.canvas.camera), index = zIndex()))
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf(Anchor(transformedLocation))
    }

    protected open fun ifVisibleNoRenderIf(): Boolean {
        return multipleElementsSelected()
    }

    protected open fun isRenderable(): Boolean {
        return true
    }
}