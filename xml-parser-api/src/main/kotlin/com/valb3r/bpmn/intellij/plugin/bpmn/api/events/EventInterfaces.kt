package com.valb3r.bpmn.intellij.plugin.bpmn.api.events

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import java.awt.geom.Point2D

interface Event
interface EventPropagatableToXml: Event
interface EventUiOnly: Event

data class EventBlock(val size: Int)

interface EventOrder<T: Event> {
    val order: Int
    val event: T
    val block: EventBlock?
}

interface LocationUpdateWithId: EventPropagatableToXml {
    val diagramElementId: DiagramElementId
    val dx: Float
    val dy: Float
    val parentElementId: DiagramElementId?
    val internalPos: Int?
}

interface NewWaypoints: EventPropagatableToXml {
    val edgeElementId: DiagramElementId
    val waypoints: List<IdentifiableWaypoint>
    val epoch: Int
}

interface DiagramElementRemoved: EventPropagatableToXml {
    val elementId: DiagramElementId
}

interface BpmnElementRemoved: EventPropagatableToXml {
    val bpmnElementId: BpmnElementId
}

interface BpmnShapeObjectAdded: EventPropagatableToXml {
    val bpmnObject: WithParentId
    val shape: ShapeElement
    val props: PropertyTable
}

interface BpmnShapeResizedAndMoved: EventPropagatableToXml {
    val diagramElementId: DiagramElementId
    val cx: Float
    val cy: Float
    val coefW: Float
    val coefH: Float

    fun transform(point: Point2D.Float): Point2D.Float
}

interface BpmnEdgeObjectAdded: EventPropagatableToXml {
    val bpmnObject: WithParentId
    val edge: EdgeWithIdentifiableWaypoints
    val props: PropertyTable
}

interface BpmnParentChanged: EventPropagatableToXml {
    val bpmnElementId: BpmnElementId
    val newParentId: BpmnElementId
    val propagateToXml: Boolean
}

interface PropertyUpdateWithId: EventPropagatableToXml {
    val bpmnElementId: BpmnElementId
    val property: PropertyType
    val newValue: Any
    val referencedValue: Any?
    val newIdValue: BpmnElementId?
    val propertyIndex: String?
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
interface IdentifiableWaypoint: Translatable<IdentifiableWaypoint>, WithDiagramId {
    val x: Float
    val y: Float
    val origX: Float
    val origY: Float
    val physical: Boolean
    val internalPhysicalPos: Int

    fun moveTo(dx: Float, dy: Float): IdentifiableWaypoint
    fun asPhysical(): IdentifiableWaypoint
    fun originalLocation(): IdentifiableWaypoint
    fun asWaypointElement(): WaypointElement
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
interface EdgeWithIdentifiableWaypoints: WithDiagramId {
    val bpmnElement: BpmnElementId?
    val waypoint: MutableList<IdentifiableWaypoint>
    val epoch: Int

    fun updateBpmnElemId(newId: BpmnElementId): EdgeWithIdentifiableWaypoints
}
