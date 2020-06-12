package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeResizedAndMovedEvent
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.EPSILON
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorBottom
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorTop
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ResizeViewTransform
import java.awt.geom.Point2D
import kotlin.math.abs

abstract class ResizeableShapeRenderElement(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        shape: ShapeElement,
        state: RenderState
) : ShapeRenderElement(elementId, bpmnElementId, shape, state) {

    private val anchors = Pair(
            ShapeResizeAnchorTop(DiagramElementId("TOP:" + shape.id.id), Point2D.Float(shape.bounds().first.x, shape.bounds().first.y), { doComputeLocationChangesBasedOnTransformationWithCascade() } , state),
            ShapeResizeAnchorBottom(DiagramElementId("BOTTOM:" + shape.id.id), Point2D.Float(shape.bounds().second.x, shape.bounds().second.y), { doComputeLocationChangesBasedOnTransformationWithCascade() }, state)
    )

    override val children: MutableList<BaseDiagramRenderElement> = mutableListOf(anchors.first, anchors.second)

    override fun afterStateChangesAppliedNoChildren() {
        // Detect only resize, not drag as drag is handled by higher-level elements and it may happen that two anchors
        // were dragged simultaneously
        val widthOrig = anchors.second.location.x - anchors.first.location.x
        val heightOrig = anchors.second.location.y - anchors.first.location.y

        val widthNew = anchors.second.transformedLocation.x - anchors.first.transformedLocation.x
        val heightNew = anchors.second.transformedLocation.y - anchors.first.transformedLocation.y

        if (abs(widthNew - widthOrig) > EPSILON || abs(heightNew - heightOrig) > EPSILON) {
            handleResize(widthOrig, heightOrig, widthNew, heightNew)
        }

        super.afterStateChangesAppliedNoChildren()
    }

    override fun doComputeLocationChangesBasedOnTransformationWithCascade(): MutableList<Event> {
        val transform = viewTransform
        if (transform !is ResizeViewTransform) {
            return mutableListOf()
        }

        val result = mutableListOf<Event>(
                BpmnShapeResizedAndMovedEvent(elementId, transform.cx, transform.cy, transform.coefW, transform.coefH)
        )

        cascadeTo.mapNotNull { state.elemMap[it.waypointId] }.forEach {
            result += it.doComputeLocationChangesBasedOnTransformationWithCascade()
        }

        return result
    }

    private fun handleResize(widthOrig: Float, heightOrig: Float, widthNew: Float, heightNew: Float) {
        when {
            anchors.first.location.distance(anchors.first.transformedLocation) < EPSILON -> {
                viewTransform = ResizeViewTransform(
                        anchors.first.location.x,
                        anchors.first.location.y,
                        widthNew / widthOrig,
                        heightNew / heightOrig
                )

            }
            anchors.second.location.distance(anchors.second.transformedLocation) < EPSILON -> {
                viewTransform = ResizeViewTransform(
                        anchors.second.location.x,
                        anchors.second.location.y,
                        widthNew / widthOrig,
                        heightNew / heightOrig
                )

            }
            else -> {
                throw IllegalStateException("Both anchors moved")
            }
        }
    }
}