package com.valb3r.bpmn.intellij.plugin.state

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.render.EdgeElementState
import java.util.concurrent.atomic.AtomicReference

private val currentStateProvider = AtomicReference<CurrentStateProvider>()

fun currentStateProvider(): CurrentStateProvider {
    return currentStateProvider.updateAndGet {
        if (null == it) {
            return@updateAndGet CurrentStateProvider()
        }

        return@updateAndGet it
    }
}

data class CurrentState(
        val shapes: List<ShapeElement>,
        val edges: List<EdgeWithIdentifiableWaypoints>,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByStaticId: Map<BpmnElementId, WithBpmnId>,
        val elemPropertiesByStaticElementId: Map<BpmnElementId, Map<PropertyType, Property>>
)

// Global singleton
class CurrentStateProvider {
    private var fileState = CurrentState(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())
    private var currentState = CurrentState(emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap())

    fun resetStateTo(processObject: BpmnProcessObjectView) {
        fileState = CurrentState(
                processObject.diagram.firstOrNull()?.bpmnPlane?.bpmnShape ?: emptyList(),
                processObject.diagram.firstOrNull()?.bpmnPlane?.bpmnEdge?.map { EdgeElementState(it) } ?: emptyList(),
                processObject.elementByDiagramId,
                processObject.elementByStaticId,
                processObject.elemPropertiesByElementId
        )
        currentState = fileState
        updateEventsRegistry().reset()
    }

    fun currentState(): CurrentState {
        val newShapes = updateEventsRegistry().newShapeElements().map { it.event }
        val newEdges = updateEventsRegistry().newEdgeElements().map { it.event }
        val newEdgeElems = updateEventsRegistry().newEdgeElements().map { it.event }

        val newElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId> = HashMap()
        val newElementByStaticId: MutableMap<BpmnElementId, WithBpmnId> = HashMap()
        val newElemPropertiesByStaticElementId: MutableMap<BpmnElementId, Map<PropertyType, Property>> = HashMap()

        newShapes.forEach {newElementByDiagramId[it.shape.id] = it.shape.bpmnElement}
        newEdgeElems.forEach {newElementByDiagramId[it.edge.id] = it.bpmnObject.id}

        newShapes.forEach {newElementByStaticId[it.bpmnObject.id] = it.bpmnObject}
        newEdges.forEach {newElementByStaticId[it.bpmnObject.id] = it.bpmnObject}

        newShapes.forEach {newElemPropertiesByStaticElementId[it.bpmnObject.id] = it.props}
        newEdges.forEach {newElemPropertiesByStaticElementId[it.bpmnObject.id] = it.props}

        // Update element names
        val allProperties = fileState.elemPropertiesByStaticElementId.toMutableMap().plus(newElemPropertiesByStaticElementId)
        val updatedProperties:  MutableMap<BpmnElementId, MutableMap<PropertyType, Property>> = HashMap()
        allProperties.forEach { prop ->
            updatedProperties[prop.key] = prop.value.toMutableMap()
            updateEventsRegistry().currentPropertyUpdateEventList(prop.key)
                    .map { it.event }
                    .filterIsInstance<StringValueUpdatedEvent>()
                    .forEach {
                        updatedProperties[prop.key]?.set(it.property, Property(it.newValue))
                    }
        }

        return CurrentState(
                shapes = fileState.shapes.toList().union(newShapes.map { it.shape })
                        .filter { !updateEventsRegistry().isDeleted(it.id) && !updateEventsRegistry().isDeleted(it.bpmnElement) }
                        .map { updateLocationAndInnerTopology(it) },
                edges = fileState.edges.toList().union(newEdgeElems.map { it.edge })
                        .filter { !updateEventsRegistry().isDeleted(it.id) && !(it.bpmnElement?.let { updateEventsRegistry().isDeleted(it) }  ?: false)}
                        .map { updateLocationAndInnerTopology(it) },
                elementByDiagramId = fileState.elementByDiagramId.toMutableMap().plus(newElementByDiagramId),
                elementByStaticId = fileState.elementByStaticId.toMutableMap().plus(newElementByStaticId),
                elemPropertiesByStaticElementId = updatedProperties
        )
    }

    private fun updateLocationAndInnerTopology(elem: ShapeElement): ShapeElement {
        val updates = updateEventsRegistry().currentLocationUpdateEventList(elem.id)
        var dx = 0.0f
        var dy = 0.0f
        updates.forEach { dx += it.event.dx; dy += it.event.dy }
        return elem.copyAndTranslate(dx, dy)
    }

    private fun updateLocationAndInnerTopology(elem: EdgeWithIdentifiableWaypoints): EdgeWithIdentifiableWaypoints {
        val hasNoCommittedAnchorUpdates = elem.waypoint.firstOrNull { updateEventsRegistry().currentLocationUpdateEventList(it.id).isNotEmpty() }
        val hasNoNewAnchors = updateEventsRegistry().newWaypointStructure(elem.id).isEmpty()
        if (null == hasNoCommittedAnchorUpdates && hasNoNewAnchors) {
            return elem
        }

        val event = updateEventsRegistry().newWaypointStructure(elem.id).lastOrNull()?.event
        val waypoints =  event?.waypoints?.toMutableList() ?: elem.waypoint.filter { it.physical }.toMutableList()
        val epoch = event?.epoch ?: 0
        val newState = EdgeElementState(elem, waypoints, epoch)
        val updatedWaypoints = newState.waypoint.filter { it.physical }.map { updateWaypointLocation(it) }
        return EdgeElementState(newState, updatedWaypoints, epoch)
    }

    private fun updateWaypointLocation(waypoint: IdentifiableWaypoint): IdentifiableWaypoint {
        val updates = updateEventsRegistry().currentLocationUpdateEventList(waypoint.id)
        var dx = 0.0f
        var dy = 0.0f
        updates.forEach { dx += it.event.dx; dy += it.event.dy }

        return waypoint.copyAndTranslate(dx, dy)
    }
}