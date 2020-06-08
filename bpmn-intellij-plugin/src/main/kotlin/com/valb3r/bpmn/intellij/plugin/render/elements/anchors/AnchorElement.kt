package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.awt.geom.Point2D

abstract class AnchorElement(
        override val elementId: DiagramElementId,
        private val currentLocation: Point2D.Float,
        state: RenderState
): BaseRenderElement(elementId, state) {

    val location: Point2D.Float
        get() = currentLocation

    val transformedLocation: Point2D.Float
        get() = viewTransform.transform(currentLocation)

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        // NOP
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        return mutableListOf()
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }
}