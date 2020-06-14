package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.internal.CascadeTranslationOrChangesToWaypoint
import java.awt.geom.Point2D
import java.util.*

class AnyShapeNestableIconShape(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        val icon: String,
        shape: ShapeElement,
        state: RenderState
) : IconShape(elementId, bpmnElementId, icon, shape, state) {

    override fun handlePossibleNestingTo(allDroppedOn: SortedMap<AreaType, BpmnElementId>, cascadeTargets: List<CascadeTranslationOrChangesToWaypoint>): MutableList<Event> {
        val nests = setOf(allDroppedOn[AreaType.SHAPE_THAT_NESTS], allDroppedOn[AreaType.SHAPE])
        val parentProcess = allDroppedOn[AreaType.PARENT_PROCESS_SHAPE]
        val currentParent = parents.firstOrNull()
        val newEvents = mutableListOf<Event>()

        if (allDroppedOn[allDroppedOn.firstKey()] == currentParent?.bpmnElementId) {
            return newEvents
        }

        nests.filterNotNull().forEach { nestTo ->
            if (nestTo != currentParent?.bpmnElementId) {
                newEvents += StringValueUpdatedEvent(shape.bpmnElement, PropertyType.ATTACHED_TO_REF, nestTo.id)
            }
        }

        if (nests.isEmpty() && null != parentProcess && parentProcess != parents.firstOrNull()?.bpmnElementId) {
            newEvents += StringValueUpdatedEvent(shape.bpmnElement, PropertyType.ATTACHED_TO_REF, parentProcess.id)
        }

        return newEvents
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }
}