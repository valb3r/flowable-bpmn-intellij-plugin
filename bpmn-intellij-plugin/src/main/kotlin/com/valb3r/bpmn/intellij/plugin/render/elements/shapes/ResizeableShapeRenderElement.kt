package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorBottom
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.ShapeResizeAnchorTop
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

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

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        // its view transform is solely defined by anchors
        return Rectangle2D.Float(
                anchors.first.transformedLocation.x,
                anchors.first.transformedLocation.y,
                anchors.second.transformedLocation.x - anchors.first.transformedLocation.x,
                anchors.second.transformedLocation.y - anchors.first.transformedLocation.y
        )
    }
}