package com.valb3r.bpmn.intellij.plugin.core.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.properties.uionly.UiOnlyPropertyType
import java.awt.geom.Point2D

data class StringValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: String, override val referencedValue: String? = null, override val newIdValue: BpmnElementId? = null): PropertyUpdateWithId

data class BooleanValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: Boolean, override val referencedValue: Boolean? = null, override val newIdValue: BpmnElementId? = null): PropertyUpdateWithId

data class DraggedToEvent(override val diagramElementId: DiagramElementId, override val dx: Float, override val dy: Float, override val parentElementId: DiagramElementId?, override val internalPos: Int?): LocationUpdateWithId

data class BpmnShapeResizedAndMovedEvent(override val diagramElementId: DiagramElementId, override val cx: Float, override val cy: Float, override val coefW: Float, override val coefH: Float): BpmnShapeResizedAndMoved {

    override fun transform(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(cx + (point.x - cx) * coefW, cy + (point.y - cy) * coefH)
    }
}

data class BpmnParentChangedEvent(override val bpmnElementId: BpmnElementId, override val newParentId: BpmnElementId, override val propagateToXml: Boolean = true): BpmnParentChanged

data class NewWaypointsEvent(override val edgeElementId: DiagramElementId, override val waypoints: List<IdentifiableWaypoint>, override val epoch: Int): NewWaypoints

data class DiagramElementRemovedEvent(override val elementId: DiagramElementId): DiagramElementRemoved

data class BpmnElementRemovedEvent(override val bpmnElementId: BpmnElementId): BpmnElementRemoved

data class BpmnShapeObjectAddedEvent(override val bpmnObject: WithParentId, override val shape: ShapeElement, override val props: Map<PropertyType, Property>): BpmnShapeObjectAdded

data class BpmnEdgeObjectAddedEvent(override val bpmnObject: WithParentId, override val edge: EdgeWithIdentifiableWaypoints, override val props: Map<PropertyType, Property>): BpmnEdgeObjectAdded

data class BooleanUiOnlyValueUpdatedEvent(val bpmnElementId: BpmnElementId, val property: UiOnlyPropertyType, val newValue: Boolean): EventUiOnly