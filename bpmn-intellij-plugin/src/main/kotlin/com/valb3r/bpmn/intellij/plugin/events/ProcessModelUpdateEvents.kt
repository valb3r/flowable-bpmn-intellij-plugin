package com.valb3r.bpmn.intellij.plugin.events

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

private val updateEvents = AtomicReference<ProcessModelUpdateEvents>()

fun updateEventsRegistry(): ProcessModelUpdateEvents {
    return updateEvents.updateAndGet {
        if (null == it) {
            return@updateAndGet ProcessModelUpdateEvents(ConcurrentHashMap())
        }

        return@updateAndGet it
    }
}

// Global singleton
class ProcessModelUpdateEvents(private val elementIdAndUpdates: MutableMap<String, MutableList<UpdateWithId>>) {

    private val fileCommitListeners: MutableList<Any> = ArrayList()

    fun commitToFile() {
    }

    fun addEvent(event: UpdateWithId) {
        elementIdAndUpdates.computeIfAbsent(event.elementId) { CopyOnWriteArrayList() } += event
    }

    fun currentEventList(elementId: String): List<UpdateWithId> {
        return elementIdAndUpdates.getOrDefault(elementId, emptyList())
    }
}