package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeResizedAndMovedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.core.render.elements.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.ShapeResizeAnchorBottom
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.ShapeResizeAnchorTop
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.PreTransformHandler
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.ResizeViewTransform
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.xmlNavigator
import java.awt.geom.Point2D
import kotlin.math.abs

abstract class ResizeableShapeRenderElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        shape: ShapeElement,
        state: () -> RenderState
) : ShapeRenderElement(elementId, bpmnElementId, shape, state) {

    private val anchors = Pair(
            ShapeResizeAnchorTop(DiagramElementId("TOP:" + shape.id.id), elementId, Point2D.Float(shape.bounds().first.x, shape.bounds().first.y), { doComputeLocationChangesBasedOnTransformationWithCascade() } , state),
            ShapeResizeAnchorBottom(DiagramElementId("BOTTOM:" + shape.id.id), elementId, Point2D.Float(shape.bounds().second.x, shape.bounds().second.y), { doComputeLocationChangesBasedOnTransformationWithCascade() }, state)
    )

    override val children: MutableList<BaseDiagramRenderElement> = mutableListOf(
            anchors.first,
            anchors.second,
            edgeExtractionAnchor
    )

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val spaceCoeff = 1.5f
        val actionCount = 3
        val start = state().ctx.canvas.camera.fromCameraView(Point2D.Float(0.0f, 0.0f))
        val end = state().ctx.canvas.camera.fromCameraView(Point2D.Float(0.0f, ACTIONS_ICO_SIZE * spaceCoeff))
        val ySpacing = end.y - start.y

        val rect = currentOnScreenRect(state().ctx.canvas.camera)
        val left = state().ctx.canvas.camera.toCameraView(Point2D.Float(rect.x, rect.y))
        val right = state().ctx.canvas.camera.toCameraView(Point2D.Float(rect.x + rect.width, rect.y + rect.height))

        if (ACTIONS_ICO_SIZE * actionCount >= (right.y - left.y)) {
            return mutableMapOf()
        }

        var currY = y
        val delId = elementId.elemIdToRemove()
        val deleteIconArea = state().ctx.canvas.drawIcon(BoundsElement(x, currY, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state().icons.recycleBin)
        state().ctx.interactionContext.clickCallbacks[delId] = { dest ->
            dest.addElementRemovedEvent(getEventsToDeleteDiagram(), getEventsToDeleteElement())
        }

        currY += spaceCoeff * ySpacing
        val toXmlId = DiagramElementId("TOXML:$elementId")
        val toXmlArea = state().ctx.canvas.drawText(Point2D.Float(x, currY), "<XML/>", Colors.INNER_TEXT_COLOR.color)
        state().ctx.interactionContext.clickCallbacks[toXmlId] = { dest -> xmlNavigator(state().ctx.project).jumpTo(bpmnElementId)}

        return mutableMapOf(
                delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId),
                toXmlId to AreaWithZindex(toXmlArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
        )
    }

    override fun afterStateChangesAppliedNoChildren() {
        detectAndHandleShapeResize()

        super.afterStateChangesAppliedNoChildren()
    }

    override fun doComputeLocationChangesBasedOnTransformationWithCascade(): MutableList<Event> {
        val transform = state().viewTransform(elementId)
        if (transform !is ResizeViewTransform) {
            return mutableListOf()
        }

        val result = mutableListOf<Event>(
                BpmnShapeResizedAndMovedEvent(elementId, transform.cx, transform.cy, transform.coefW, transform.coefH)
        )

        cascadeTo.mapNotNull { state().elemMap[it.waypointId] }.forEach {
            result += it.doComputeLocationChangesBasedOnTransformationWithCascade()
        }

        return result
    }

    private fun detectAndHandleShapeResize() {
        // Detect only resize, not drag as drag is handled by higher-level elements and it may happen that two anchors
        // were dragged simultaneously
        val widthOrig = anchors.second.location.x - anchors.first.location.x
        val heightOrig = anchors.second.location.y - anchors.first.location.y

        val widthNew = anchors.second.transformedLocation.x - anchors.first.transformedLocation.x
        val heightNew = anchors.second.transformedLocation.y - anchors.first.transformedLocation.y

        if (abs(widthNew - widthOrig) > EPSILON || abs(heightNew - heightOrig) > EPSILON) {
            handleResize(widthOrig, heightOrig, widthNew, heightNew)
        }
    }

    private fun handleResize(widthOrig: Float, heightOrig: Float, widthNew: Float, heightNew: Float) {
        when {
            anchors.first.location.distance(anchors.first.transformedLocation) < EPSILON -> {
                state().viewTransforms[elementId] = ResizeViewTransform(
                        anchors.first.location.x,
                        anchors.first.location.y,
                        widthNew / widthOrig,
                        heightNew / heightOrig,
                        PreTransformHandler(mutableListOf(state().viewTransform(elementId)))
                )
            }

            anchors.second.location.distance(anchors.second.transformedLocation) < EPSILON -> {
                state().viewTransforms[elementId] = ResizeViewTransform(
                        anchors.second.location.x,
                        anchors.second.location.y,
                        widthNew / widthOrig,
                        heightNew / heightOrig,
                        PreTransformHandler(mutableListOf(state().viewTransform(elementId)))
                )

            }
        }
    }
}