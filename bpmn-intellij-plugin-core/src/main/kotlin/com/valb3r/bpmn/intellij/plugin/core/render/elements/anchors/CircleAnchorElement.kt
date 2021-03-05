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
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.RectangleTransformationIntrospection
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class CircleAnchorElement(
        elementId: DiagramElementId,
        attachedTo: DiagramElementId?,
        currentLocation: Point2D.Float,
        private val radius: Float,
        private val bodyColor: Colors,
        state: RenderState
) : AnchorElement(elementId, attachedTo, currentLocation, state) {

    override val areaType: AreaType
        get() = AreaType.POINT

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        return viewTransform.transform(
                elementId,
                RectangleTransformationIntrospection(
                        Rectangle2D.Float(
                                location.x - radius,
                                location.y - radius,
                                2.0f * radius,
                                2.0f * radius
                        ),
                        AreaType.POINT
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
                    elementId to AreaWithZindex(Area(Rectangle2D.Float(point.x, point.y, 0.1f, 0.1f)), areaType, waypointAnchors(ctx.canvas.camera), index = zIndex())
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
        return mutableMapOf(elementId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), index = zIndex()))
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