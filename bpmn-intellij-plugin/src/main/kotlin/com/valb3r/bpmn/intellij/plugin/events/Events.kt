package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.render.WaypointElementState

data class StringValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, val newValue: String): PropertyUpdateWithId

data class BooleanValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, val newValue: Boolean): PropertyUpdateWithId

data class DraggedToEvent(override val diagramElementId: DiagramElementId, override val dx: Float, override val dy: Float): LocationUpdateWithId

data class NewWaypointsEvent(override val edgeElementId: DiagramElementId, override val waypoints: List<WaypointElementState>): NewWaypoints

data class DiagramElementRemovedEvent(override val elementId: DiagramElementId): DiagramElementRemoved

data class BpmnElementRemovedEvent(override val elementId: BpmnElementId): BpmnElementRemoved

data class BpmnShapeObjectAddedEvent(override val bpmnObject: WithBpmnId, override val shape: ShapeElement, override val props: Map<PropertyType, Property>): BpmnShapeObjectAdded

data class BpmnEdgeObjectAddedEvent(override val bpmnObject: WithBpmnId, override val edge: EdgeElement, override val props: Map<PropertyType, Property>): BpmnEdgeObjectAdded

data class CommittedToFile(val eventCount: Int): Event