package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.EPSILON
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorBottom
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorTop
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ResizeViewTransform
import java.awt.geom.Point2D
import kotlin.math.abs

abstract class ResizeableShapeRenderElement(
        override val elementId: DiagramElementId,
        shape: ShapeElement,
        state: RenderState
) : ShapeRenderElement(elementId, shape, state) {

    private val anchors = Pair(
            ShapeResizeAnchorTop(DiagramElementId("TOP:" + shape.id.id), Point2D.Float(shape.bounds().first.x, shape.bounds().first.y), state),
            ShapeResizeAnchorBottom(DiagramElementId("BOTTOM:" + shape.id.id), Point2D.Float(shape.bounds().second.x, shape.bounds().second.y), state)
    )

    override val children: MutableList<BaseRenderElement> = mutableListOf(anchors.first, anchors.second)

    override fun afterStateChangesAppliedNoChildren(elemMap: Map<DiagramElementId, BaseRenderElement>) {
        // Detect only resize, not drag as drag is handled by higher-level elements and it may happen that two anchors
        // were dragged simultaneously
        val widthOrig = anchors.second.location.x - anchors.first.location.x
        val heightOrig = anchors.second.location.y - anchors.first.location.y

        val widthNew = anchors.second.transformedLocation.x - anchors.first.transformedLocation.x
        val heightNew = anchors.second.transformedLocation.y - anchors.first.transformedLocation.y

        if (abs(widthNew - widthOrig) > EPSILON || abs(heightNew - heightOrig) > EPSILON) {
            handleResize(widthOrig, heightOrig, widthNew, heightNew)
        }

        super.afterStateChangesAppliedNoChildren(elemMap)
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