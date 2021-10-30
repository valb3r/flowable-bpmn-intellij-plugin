package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.internal.CascadeTranslationOrChangesToWaypoint
import java.awt.geom.Point2D

class AnyShapeNestableIconShape(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        val icon: String,
        shape: ShapeElement,
        state: () -> RenderState
) : IconShape(elementId, bpmnElementId, icon, shape, state) {

    override val areaType: AreaType
        get() = AreaType.SELECTS_DRAG_TARGET

    fun handleParentNestingChange(droppedOn: BpmnElementId): MutableList<Event> {
        val newEvents = mutableListOf<Event>()
        val currentParent = parents.firstOrNull()

        if (droppedOn == currentParent?.bpmnElementId) {
            return newEvents
        }

        return mutableListOf(
            BpmnParentChangedEvent(shape.bpmnElement, droppedOn, true),
            BpmnParentChangedEvent(shape.bpmnElement, currentParent?.bpmnElementId!!, false) // compensate parent change for UI only (it still stays a child of its parent)
        )
    }

    override fun handlePossibleNestingTo(allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>, cascadeTargets: List<CascadeTranslationOrChangesToWaypoint>): MutableList<Event> {
        val allDroppedOn = linkedMapOf<AreaType, BpmnElementId>()
        allDroppedOnAreas.forEach { if (!allDroppedOn.containsKey(it.value.areaType)) allDroppedOn[it.value.areaType] = it.key}
        val nests = listOfNotNull(allDroppedOn[AreaType.SHAPE], allDroppedOn[AreaType.SHAPE_THAT_NESTS], allDroppedOn[AreaType.PARENT_PROCESS_SHAPE])
        val xmlNest = allDroppedOn[AreaType.SHAPE_THAT_NESTS] ?: allDroppedOn[AreaType.PARENT_PROCESS_SHAPE]!!
        val currentParent = parents.firstOrNull()
        val newEvents = mutableListOf<Event>()

        if (allDroppedOn[allDroppedOn.keys.first()] == currentParent?.bpmnElementId) {
            return newEvents
        }

        for (nestTo in nests) {
            if (nestTo == currentParent?.bpmnElementId) {
                continue
            }

            newEvents += BpmnParentChangedEvent(shape.bpmnElement, xmlNest, true)
            newEvents += BpmnParentChangedEvent(shape.bpmnElement, nestTo, false)
            newEvents += StringValueUpdatedEvent(shape.bpmnElement, PropertyType.ATTACHED_TO_REF, nestTo.id)
            break
        }

        return newEvents
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        val rect = currentOnScreenRect(camera)
        val halfWidth = rect.width / 2.0f
        val halfHeight = rect.height / 2.0f

        val cx = rect.x + rect.width / 2.0f
        val cy = rect.y + rect.height / 2.0f
        return mutableSetOf(
                Anchor(Point2D.Float(cx - halfWidth, cy)),
                Anchor(Point2D.Float(cx + halfWidth, cy)),
                Anchor(Point2D.Float(cx, cy - halfHeight)),
                Anchor(Point2D.Float(cx, cy + halfHeight))
        )
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun parentForRelatedSequenceElem(): BaseBpmnRenderElement {
        if (parents.first().parents.isEmpty()) {
            return parents.first()
        }

        return parents.first().parents.first()
    }
}