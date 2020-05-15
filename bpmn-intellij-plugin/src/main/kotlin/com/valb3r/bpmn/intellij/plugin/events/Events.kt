package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

data class StringValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: String): PropertyUpdateWithId

data class BooleanValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: Boolean): PropertyUpdateWithId

data class DraggedToEvent(override val diagramElementId: DiagramElementId, override val dx: Float, override val dy: Float, override val parentElementId: DiagramElementId?, override val internalPos: Int?): LocationUpdateWithId

data class NewWaypointsEvent(override val edgeElementId: DiagramElementId, override val waypoints: List<IdentifiableWaypoint>, override val epoch: Int): NewWaypoints

data class DiagramElementRemovedEvent(override val elementId: DiagramElementId): DiagramElementRemoved

data class BpmnElementRemovedEvent(override val elementId: BpmnElementId): BpmnElementRemoved

data class BpmnShapeObjectAddedEvent(override val bpmnObject: WithBpmnId, override val shape: ShapeElement, override val props: Map<PropertyType, Property>): BpmnShapeObjectAdded

data class BpmnEdgeObjectAddedEvent(override val bpmnObject: WithBpmnId, override val edge: EdgeWithIdentifiableWaypoints, override val props: Map<PropertyType, Property>): BpmnEdgeObjectAdded

data class CommittedToFile(val eventCount: Int): Event