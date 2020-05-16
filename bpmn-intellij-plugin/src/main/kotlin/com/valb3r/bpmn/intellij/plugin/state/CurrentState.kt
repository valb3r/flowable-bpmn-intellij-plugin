package com.valb3r.bpmn.intellij.plugin.state

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
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
        return handleUpdates(currentState)
    }

    private fun handleUpdates(state: CurrentState): CurrentState {
        var updatedShapes = state.shapes.toMutableList()
        var updatedEdges = state.edges.toMutableList()
        val updatedElementByDiagramId = state.elementByDiagramId.toMutableMap()
        val updatedElementByStaticId = state.elementByStaticId.toMutableMap()
        val updatedElemPropertiesByStaticElementId = state.elemPropertiesByStaticElementId.toMutableMap()

        val updates = updateEventsRegistry().updateEventList()

        updates.map { it.event }.forEach { event ->
            when (event) {
                is LocationUpdateWithId -> {
                    updatedShapes = updatedShapes.map { shape -> if (shape.id == event.diagramElementId) updateWaypointLocation(shape, event) else shape }.toMutableList()
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.parentElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is NewWaypoints -> {
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.edgeElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is BpmnElementRemoved -> {
                    val shape = updatedShapes.find { it.bpmnElement == event.elementId }
                    val edge = updatedEdges.find { it.bpmnElement == event.elementId }
                    shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
                    edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
                    updatedElementByStaticId.remove(event.elementId)
                    updatedElemPropertiesByStaticElementId.remove(event.elementId)
                }
                is DiagramElementRemoved -> {
                    val shape = updatedShapes.find { it.id == event.elementId }
                    val edge = updatedEdges.find { it.id == event.elementId }
                    shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
                    edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
                    updatedElementByDiagramId.remove(event.elementId)
                    updatedElementByDiagramId.remove(event.elementId)
                }
                is BpmnShapeObjectAdded -> {
                    updatedShapes.add(event.shape)
                    updatedElementByDiagramId[event.shape.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props
                }
                is BpmnEdgeObjectAdded -> {
                    updatedEdges.add(event.edge)
                    updatedElementByDiagramId[event.edge.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props
                }
                is PropertyUpdateWithId -> {
                    val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId]?.toMutableMap() ?: mutableMapOf()
                    updated.set(event.property, Property(event.newValue))
                    updatedElemPropertiesByStaticElementId[event.bpmnElementId] = updated
                }
            }
        }

        return CurrentState(
                updatedShapes,
                updatedEdges,
                updatedElementByDiagramId,
                updatedElementByStaticId,
                updatedElemPropertiesByStaticElementId
        )
    }

    private fun updateWaypointLocation(elem: ShapeElement, update: LocationUpdateWithId): ShapeElement {
        return elem.copyAndTranslate(update.dx, update.dy)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: LocationUpdateWithId): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, elem.waypoint.filter { it.physical }.map { if (it.id == update.diagramElementId ) it.copyAndTranslate(update.dx, update.dy) else it }, elem.epoch)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: NewWaypoints): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, update.waypoints, update.epoch)
    }
}