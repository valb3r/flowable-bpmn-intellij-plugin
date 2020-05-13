package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.render.WaypointElementState

interface Event

interface EventOrder<T: Event> {
    val order: Long
    val event: T
}

interface LocationUpdateWithId: Event {
    val diagramElementId: DiagramElementId
    val dx: Float
    val dy: Float
}

interface NewWaypoints: Event {
    val edgeElementId: DiagramElementId
    val waypoints: List<WaypointElementState>
}

interface DiagramElementRemoved: Event {
    val elementId: DiagramElementId
}

interface BpmnElementRemoved: Event {
    val elementId: BpmnElementId
}

interface BpmnShapeObjectAdded: Event {
    val bpmnObject: WithBpmnId
    val shape: ShapeElement
    val props: Map<PropertyType, Property>
}

interface BpmnEdgeObjectAdded: Event {
    val bpmnObject: WithBpmnId
    val edge: EdgeElement
    val props: Map<PropertyType, Property>
}

interface PropertyUpdateWithId: Event {
    val bpmnElementId: BpmnElementId
    val property: PropertyType
}