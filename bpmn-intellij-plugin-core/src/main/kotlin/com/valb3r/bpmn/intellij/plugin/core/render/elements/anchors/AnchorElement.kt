package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.PointTransformationIntrospection
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class AnchorElement(
        elementId: DiagramElementId,
        protected val attachedTo: DiagramElementId?,
        protected val currentLocation: Point2D.Float,
        state: () -> RenderState
): BaseDiagramRenderElement(elementId, state) {

    val location: Point2D.Float
        get() = currentLocation

    val transformedLocation: Point2D.Float
        get() = state().viewTransform(elementId).transform(elementId, currentLocation, PointTransformationIntrospection(attachedTo, viewTransformLevel))

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        // NOP
        return mutableMapOf()
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        // NOP
        return mutableListOf()
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        // NOP
    }

    override fun currentRect(): Rectangle2D.Float {
        return Rectangle2D.Float(location.x, location.y, 0.0f, 0.0f)
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        return mutableListOf()
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun getEventsToDeleteDiagram(): List<DiagramElementRemovedEvent> {
        return listOf()
    }

    override fun getEventsToDeleteElement(): List<BpmnElementRemovedEvent> {
        return listOf()
    }
}
