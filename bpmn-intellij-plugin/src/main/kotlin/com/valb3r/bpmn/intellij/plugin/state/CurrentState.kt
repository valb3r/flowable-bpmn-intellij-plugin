package com.valb3r.bpmn.intellij.plugin.state

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.render.dto.EdgeElementState
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
        val edges: List<EdgeElementState>,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByStaticId: Map<BpmnElementId, WithId>,
        val elemPropertiesByStaticElementId: Map<BpmnElementId, Map<PropertyType, Property>>
)

// Global singleton
class CurrentStateProvider {

    private val updateEvents = updateEventsRegistry()
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
    }

    fun currentState(): CurrentState {
        return fileState.copy(
                shapes = fileState.shapes.map { updateLocation(it) },
                edges = fileState.edges.map { updateLocation(it) }
        )
    }

    private fun updateLocation(elem: ShapeElement): ShapeElement {
        val updates = updateEvents.currentLocationUpdateEventList(elem.id)
        var dx = 0.0f
        var dy = 0.0f
        updates.forEach { dx += it.dx; dy += it.dy }
        return elem.copyAndTranslate(dx, dy)
    }

    private fun updateLocation(elem: EdgeElementState): EdgeElementState {
        val updates = updateEvents.currentLocationUpdateEventList(elem.id)
        var dx = 0.0f
        var dy = 0.0f
        updates.forEach { dx += it.dx; dy += it.dy }
        return elem.copy(waypoint = ArrayList(elem.waypoint.map { it.copyAndTranslate(dx, dy) }))
    }
}