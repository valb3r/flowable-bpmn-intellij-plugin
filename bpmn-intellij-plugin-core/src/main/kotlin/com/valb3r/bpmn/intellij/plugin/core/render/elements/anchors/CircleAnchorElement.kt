package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.RectangleTransformationIntrospection
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.SelectElements
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.currentUiEventBus
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

val edgeSelectionIconIdPrefix = ":EDGE_SELECTION"

abstract class CircleAnchorElement(
        elementId: DiagramElementId,
        attachedTo: DiagramElementId?,
        currentLocation: Point2D.Float,
        private val radius: Float,
        private val bodyColor: Colors,
        state: () -> RenderState
) : AnchorElement(elementId, attachedTo, currentLocation, state) {

    override val areaType: AreaType
        get() = AreaType.POINT

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        return state().viewTransform(elementId).transform(
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

        state().ctx.interactionContext.dragEndCallbacks[elementId] = {
            dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOn: Map<BpmnElementId, AreaWithZindex> -> onDragEnd(dx, dy, droppedOn, allDroppedOn)
        }
        return mutableMapOf(elementId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), index = zIndex()))
    }

    protected fun addEdgeSelectionButton(lastMenuElem: BoundsElement, result: MutableMap<DiagramElementId, AreaWithZindex>): BoundsElement {
        val lastIconLeft = state().ctx.canvas.camera.toCameraView(Point2D.Float(lastMenuElem.x, lastMenuElem.y))
        val lastIconEndInCamera = state().ctx.canvas.camera.fromCameraView(Point2D.Float(lastIconLeft.x, lastIconLeft.y + lastMenuElem.height))
        val bounds = BoundsElement(lastIconEndInCamera.x, lastIconEndInCamera.y, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE)
        val rightAngleIcon = state().ctx.canvas.drawIcon(bounds, state().icons.selectParentSequence)
        val edgeSelectionId = DiagramElementId(edgeSelectionIconIdPrefix + elementId.id)
        state().ctx.interactionContext.postClickCallbacks[edgeSelectionId] = { dest ->
            currentUiEventBus(state().ctx.project).publish(SelectElements(setOf(parents[0].elementId)))
        }

        result += edgeSelectionId to AreaWithZindex(rightAngleIcon, areaType, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
        return bounds
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