package com.valb3r.bpmn.intellij.plugin.events

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventOrder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.LocationUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val updateEvents = AtomicReference<ProcessModelUpdateEvents>()

fun setUpdateEventsRegistry(parser: BpmnParser, file: VirtualFile) {
    updateEvents.set(ProcessModelUpdateEvents(parser, file, CopyOnWriteArrayList()))
}

fun updateEventsRegistry(): ProcessModelUpdateEvents {
    return updateEvents.get()!!
}

// Global singleton
class ProcessModelUpdateEvents(private val parser: BpmnParser, private val file: VirtualFile, private val updates: MutableList<Order<out Event>>) {

    private val order: AtomicLong = AtomicLong()
    private val fileCommitListeners: MutableList<Any> = ArrayList()
    private val broadcastPropertyEvents: MutableList<Order<PropertyUpdateWithId>> = CopyOnWriteArrayList()
    private val parentCreatesByStaticId: MutableMap<DiagramElementId, MutableList<Order<out Event>>> = ConcurrentHashMap()
    private val locationUpdatesByStaticId: MutableMap<DiagramElementId, MutableList<Order<out Event>>> = ConcurrentHashMap()
    private val propertyUpdatesByStaticId: MutableMap<BpmnElementId, MutableList<Order<out Event>>> = ConcurrentHashMap()
    private val newShapeElements: MutableList<Order<BpmnShapeObjectAddedEvent>> = CopyOnWriteArrayList()
    private val newDiagramElements: MutableList<Order<BpmnEdgeObjectAddedEvent>> = CopyOnWriteArrayList()
    private val deletionsByStaticId: MutableMap<DiagramElementId, MutableList<Order<out Event>>> = ConcurrentHashMap()
    private val deletionsByStaticBpmnId: MutableMap<BpmnElementId, MutableList<Order<out Event>>> = ConcurrentHashMap()

    @Synchronized
    fun reset() {
        order.set(0)
        fileCommitListeners.clear()
        parentCreatesByStaticId.clear()
        locationUpdatesByStaticId.clear()
        propertyUpdatesByStaticId.clear()
        newShapeElements.clear()
        newDiagramElements.clear()
        deletionsByStaticId.clear()
        deletionsByStaticBpmnId.clear()
        broadcastPropertyEvents.clear()
    }

    fun commitToFile() {
        val lastCommit = updates.filter { it.event is CommittedToFile }.maxBy { it.order }
        val bos = ByteArrayOutputStream()
        file.inputStream.use {input ->
            parser.update(input, bos, updates.filter { it.order > (lastCommit?.order ?: -1) }.map { it.event })
        }
        val toStore = Order(order.getAndIncrement(), CommittedToFile(0))
        updates.add(toStore)

        WriteAction.run<IOException> {
            file.getOutputStream(null).use {
                it.write(bos.toByteArray())
            }
        }
    }

    fun updateEventList(): List<Order<out Event>> {
        return updates.toList()
    }

    fun addPropertyUpdateEvent(event: PropertyUpdateWithId) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        propertyUpdatesByStaticId.computeIfAbsent(event.bpmnElementId) { CopyOnWriteArrayList() } += toStore

        if (event.property.cascades) {
            broadcastPropertyEvents.add(toStore)
        }

        commitToFile()
    }

    fun addLocationUpdateEvent(event: LocationUpdateWithId) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        locationUpdatesByStaticId.computeIfAbsent(event.diagramElementId) { CopyOnWriteArrayList() } += toStore
        commitToFile()
    }

    fun addWaypointStructureUpdate(event: NewWaypointsEvent) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        parentCreatesByStaticId.computeIfAbsent(event.edgeElementId) { CopyOnWriteArrayList() } += toStore
        commitToFile()
    }

    fun addElementRemovedEvent(event: DiagramElementRemovedEvent) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        deletionsByStaticId.computeIfAbsent(event.elementId) { CopyOnWriteArrayList() } += toStore
        commitToFile()
    }

    fun addElementRemovedEvent(event: BpmnElementRemovedEvent) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        deletionsByStaticBpmnId.computeIfAbsent(event.elementId) { CopyOnWriteArrayList() } += toStore
        commitToFile()
    }

    fun addObjectEvent(event: BpmnShapeObjectAddedEvent) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        newShapeElements.add(toStore)
        commitToFile()
    }

    fun addObjectEvent(event: BpmnEdgeObjectAddedEvent) {
        val toStore = Order(order.getAndIncrement(), event)
        updates.add(toStore)
        newDiagramElements.add(toStore)
        commitToFile()
    }

    fun currentPropertyUpdateEventListWithCascaded(elementId: BpmnElementId): List<EventOrder<PropertyUpdateWithId>> {
        val latestRemoval = lastDeletion(elementId)
        val allEvents = propertyUpdatesByStaticId
                .getOrDefault(elementId, emptyList<Order<PropertyUpdateWithId>>())
                .filterIsInstance<Order<PropertyUpdateWithId>>()
                .toMutableList();
        allEvents.addAll(broadcastPropertyEvents)

        return allEvents.filter { it.order >  latestRemoval.order}
    }

    fun currentPropertyUpdateEventList(elementId: BpmnElementId): List<EventOrder<PropertyUpdateWithId>> {
        val latestRemoval = lastDeletion(elementId)
        return propertyUpdatesByStaticId
                .getOrDefault(elementId, emptyList<Order<PropertyUpdateWithId>>())
                .filterIsInstance<Order<PropertyUpdateWithId>>()
                .filter { it.order >  latestRemoval.order}
    }

    fun currentLocationUpdateEventList(elementId: DiagramElementId): List<EventOrder<LocationUpdateWithId>> {
        val latestRemoval = lastDeletion(elementId)
        return locationUpdatesByStaticId
                .getOrDefault(elementId, emptyList<Order<LocationUpdateWithId>>())
                .filterIsInstance<Order<LocationUpdateWithId>>()
                .filter { it.order >  latestRemoval.order}
    }

    fun newWaypointStructure(parentElementId: DiagramElementId): List<EventOrder<NewWaypointsEvent>> {
        val latestRemoval = lastDeletion(parentElementId)
        return parentCreatesByStaticId
                .getOrDefault(parentElementId, emptyList<Order<NewWaypointsEvent>>())
                .filterIsInstance<Order<NewWaypointsEvent>>()
                .filter { it.order >  latestRemoval.order}
    }

    fun newShapeElements(): List<EventOrder<BpmnShapeObjectAddedEvent>> {
        return newShapeElements
                .filter { it.order > lastDeletion(it.event.bpmnObject.id).order}
    }

    fun newEdgeElements(): List<EventOrder<BpmnEdgeObjectAddedEvent>> {
        return newDiagramElements
                .filter { it.order > lastDeletion(it.event.bpmnObject.id).order}
    }

    fun isDeleted(elementId: DiagramElementId): Boolean {
        return lastDeletion(elementId).event !is NullEvent
    }

    fun isDeleted(elementId: BpmnElementId): Boolean {
        return lastDeletion(elementId).event !is NullEvent
    }

    private fun lastDeletion(elementId: DiagramElementId): Order<out Event> {
        return deletionsByStaticId[elementId]?.maxBy { it.order } ?: Order(-1, NullEvent(elementId.id))
    }

    private fun lastDeletion(elementId: BpmnElementId): Order<out Event> {
        return deletionsByStaticBpmnId[elementId]?.maxBy { it.order } ?: Order(-1, NullEvent(elementId.id))
    }

    data class Order<T: Event>(override val order: Long, override val event: T): EventOrder<T>
    data class NullEvent(val forId: String): Event
}