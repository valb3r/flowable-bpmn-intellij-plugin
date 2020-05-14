package com.valb3r.bpmn.intellij.plugin.bpmn.api.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
interface Event

interface EventOrder<T: Event> {
    val order: Long
    val event: T
}

interface LocationUpdateWithId: Event {
    val diagramElementId: DiagramElementId
    val dx: Float
    val dy: Float
    val parentElementId: DiagramElementId?
    val internalPos: Int?
}

interface NewWaypoints: Event {
    val edgeElementId: DiagramElementId
    val waypoints: List<IdentifiableWaypoint>
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
    val edge: EdgeWithIdentifiableWaypoints
    val props: Map<PropertyType, Property>
}

interface PropertyUpdateWithId: Event {
    val bpmnElementId: BpmnElementId
    val property: PropertyType
    val newValue: Any
}

interface IdentifiableWaypoint: Translatable<IdentifiableWaypoint>, WithDiagramId {
    val x: Float
    val y: Float
    val origX: Float
    val origY: Float
    val physical: Boolean
    val internalPos: Int

    fun moveTo(dx: Float, dy: Float): IdentifiableWaypoint
    fun asPhysical(): IdentifiableWaypoint
    fun originalLocation(): IdentifiableWaypoint
    fun asWaypointElement(): WaypointElement
}

interface EdgeWithIdentifiableWaypoints: WithDiagramId {
    val bpmnElement: BpmnElementId?
    val waypoint: MutableList<IdentifiableWaypoint>
}