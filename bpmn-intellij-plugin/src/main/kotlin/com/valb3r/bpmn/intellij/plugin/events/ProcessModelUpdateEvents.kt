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
    private val updatesByStaticElemId: MutableMap<String, MutableList<Event>> = ConcurrentHashMap()

    fun commitToFile() {
    }

    fun addPropertyUpdateEvent(event: PropertyUpdateWithId) {
        addEvent(event.bpmnElementId.id, event)
    }

    fun addLocationUpdateEvent(event: LocationUpdateWithId) {
        addEvent(event.diagramElementId.id, event)
    }

    fun currentPropertyUpdateEventList(elementId: BpmnElementId): List<PropertyUpdateWithId> {
        return updatesByStaticElemId
                .getOrDefault(elementId.id, emptyList<PropertyUpdateWithId>())
                .filterIsInstance<PropertyUpdateWithId>()
    }

    fun currentLocationUpdateEventList(elementId: DiagramElementId): List<LocationUpdateWithId> {
        return updatesByStaticElemId
                .getOrDefault(elementId.id, emptyList<LocationUpdateWithId>())
                .filterIsInstance<LocationUpdateWithId>()
    }

    private fun addEvent(staticElemId: String, event: Event) {
        updates.add(event)
        updatesByStaticElemId.computeIfAbsent(staticElemId) { CopyOnWriteArrayList() } += event
    }
}