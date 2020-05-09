package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

private val updateEvents = AtomicReference<ProcessModelUpdateEvents>()

fun updateEventsRegistry(): ProcessModelUpdateEvents {
    return updateEvents.updateAndGet {
        if (null == it) {
            return@updateAndGet ProcessModelUpdateEvents(CopyOnWriteArrayList())
        }

        return@updateAndGet it
    }
}

// Global singleton
class ProcessModelUpdateEvents(private val updates: MutableList<Event>) {

    private val fileCommitListeners: MutableList<Any> = ArrayList()
    private val locationUpdatesByStaticId: MutableMap<DiagramElementId, MutableList<Event>> = ConcurrentHashMap()
    private val propertyUpdatesByStaticId: MutableMap<BpmnElementId, MutableList<Event>> = ConcurrentHashMap()

    fun commitToFile() {
    }

    fun addPropertyUpdateEvent(event: PropertyUpdateWithId) {
        updates.add(event)
        propertyUpdatesByStaticId.computeIfAbsent(event.bpmnElementId) { CopyOnWriteArrayList() } += event
    }

    fun addLocationUpdateEvent(event: LocationUpdateWithId) {
        updates.add(event)
        locationUpdatesByStaticId.computeIfAbsent(event.diagramElementId) { CopyOnWriteArrayList() } += event
    }

    fun currentPropertyUpdateEventList(elementId: BpmnElementId): List<PropertyUpdateWithId> {
        return propertyUpdatesByStaticId
                .getOrDefault(elementId, emptyList<PropertyUpdateWithId>())
                .filterIsInstance<PropertyUpdateWithId>()
    }

    fun currentLocationUpdateEventList(elementId: DiagramElementId): List<LocationUpdateWithId> {
        return locationUpdatesByStaticId
                .getOrDefault(elementId, emptyList<LocationUpdateWithId>())
                .filterIsInstance<LocationUpdateWithId>()
    }
}