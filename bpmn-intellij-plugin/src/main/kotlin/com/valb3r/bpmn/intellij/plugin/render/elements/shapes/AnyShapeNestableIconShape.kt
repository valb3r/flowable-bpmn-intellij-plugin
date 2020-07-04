package com.valb3r.bpmn.intellij.plugin.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.internal.CascadeTranslationOrChangesToWaypoint

class AnyShapeNestableIconShape(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        val icon: String,
        shape: ShapeElement,
        state: RenderState
) : IconShape(elementId, bpmnElementId, icon, shape, state) {

    override fun handlePossibleNestingTo(allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>, cascadeTargets: List<CascadeTranslationOrChangesToWaypoint>): MutableList<Event> {
        val allDroppedOn = linkedMapOf<AreaType, BpmnElementId>()
        allDroppedOnAreas.forEach { if (!allDroppedOn.containsKey(it.value.areaType)) allDroppedOn[it.value.areaType] = it.key}
        val nests = setOf(allDroppedOn[AreaType.SHAPE_THAT_NESTS], allDroppedOn[AreaType.SHAPE])
        val parentProcess = allDroppedOn[AreaType.PARENT_PROCESS_SHAPE]
        val currentParent = parents.firstOrNull()
        val newEvents = mutableListOf<Event>()

        if (allDroppedOn[allDroppedOn.keys.first()] == currentParent?.bpmnElementId) {
            return newEvents
        }

        nests.filterNotNull().forEach { nestTo ->
            if (nestTo != currentParent?.bpmnElementId) {
                newEvents += BpmnParentChangedEvent(shape.bpmnElement, nestTo, false)
                newEvents += StringValueUpdatedEvent(shape.bpmnElement, PropertyType.ATTACHED_TO_REF, nestTo.id)
            }
        }

        if (nests.isEmpty() && null != parentProcess && parentProcess != parents.firstOrNull()?.bpmnElementId) {
            newEvents += BpmnParentChangedEvent(shape.bpmnElement, parentProcess, false)
            newEvents += StringValueUpdatedEvent(shape.bpmnElement, PropertyType.ATTACHED_TO_REF, parentProcess.id)
        }

        return newEvents
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun areaType(): AreaType {
        return AreaType.SHAPE_ATTACHED_TO_ELEMENT
    }
}