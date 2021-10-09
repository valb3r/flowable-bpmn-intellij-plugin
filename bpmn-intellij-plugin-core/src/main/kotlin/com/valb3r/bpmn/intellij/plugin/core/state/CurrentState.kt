package com.valb3r.bpmn.intellij.plugin.core.state

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.mappers.MapTransactionalSubprocessToSubprocess
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.properties.uionly.UiOnlyPropertyType
import com.valb3r.bpmn.intellij.plugin.core.render.EdgeElementState
import org.mapstruct.factory.Mappers
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val currentStateProvider = Collections.synchronizedMap(WeakHashMap<Project,  CurrentStateProvider>())

fun currentStateProvider(project: Project): CurrentStateProvider {
    return currentStateProvider.computeIfAbsent(project) {
        CurrentStateProvider(project)
    }
}

data class CurrentState(
        var processId: BpmnElementId,
        val shapes: List<ShapeElement>,
        val edges: List<EdgeWithIdentifiableWaypoints>,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByBpmnId: Map<BpmnElementId, WithParentId>,
        val elemPropertiesByStaticElementId: Map<BpmnElementId, PropertyTable>,
        val propertyWithElementByPropertyType: Map<PropertyType, Map<BpmnElementId, Property>>,
        val elemUiOnlyPropertiesByStaticElementId: Map<BpmnElementId, Map<UiOnlyPropertyType, Property>>,
        val undoRedo: Set<ProcessModelUpdateEvents.UndoRedo>,
        val version: Long,
        val diagramByElementId: Map<BpmnElementId, DiagramElementId> = elementByDiagramId.map { Pair(it.value, it.key) }.toMap(),
) {
    fun processDiagramId(): DiagramElementId {
        return processDiagramId(processId)
    }

    companion object {
        fun processDiagramId(processId: BpmnElementId): DiagramElementId {
            return DiagramElementId(processId.id)
        }
    }
}

// Global singleton
class CurrentStateProvider(private val project: Project) {
    private var fileState = CurrentState(BpmnElementId(""), emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), 0L)
    private var currentState = CurrentState(BpmnElementId(""), emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), 0L)
    private val version = AtomicLong(0L)

    fun resetStateTo(fileContent: String, processObject: BpmnProcessObjectView) {
        version.set(0L)
        fileState = CurrentState(
                processObject.processId,
                processObject.diagram.flatMap { it.bpmnPlane.bpmnShape ?: emptyList() },
                processObject.diagram.flatMap { it.bpmnPlane.bpmnEdge ?: emptyList() }.map { EdgeElementState(it) },
                processObject.elementByDiagramId,
                processObject.elementByStaticId,
                processObject.elemPropertiesByElementId,
                emptyMap(),
                emptyMap(),
                emptySet(),
                0L
        )
        currentState = fileState
        updateEventsRegistry(project).reset(fileContent)
    }

    fun currentState(): CurrentState {
        return handleUpdates(currentState)
    }

    private fun handleUpdates(state: CurrentState): CurrentState {
        var updatedShapes = state.shapes.toMutableList()
        var updatedEdges = state.edges.toMutableList()
        val updatedElementByDiagramId = state.elementByDiagramId.toMutableMap()
        val updatedElementByStaticId = state.elementByBpmnId.toMutableMap()
        val updatedElemPropertiesByStaticElementId = state.elemPropertiesByStaticElementId.mapValues { it.value.copy() }.toMutableMap()
        val updatedPropertyWithElementByPropertyType = mutableMapOf<PropertyType, MutableMap<BpmnElementId, Property>>()
        val updatedElemUiOnlyPropertiesByStaticElementId = state.elemUiOnlyPropertiesByStaticElementId.toMutableMap()
        var updatedProcessId = state.processId

        val updateEventsRegistry: ProcessModelUpdateEvents = updateEventsRegistry(project)
        val undoRedoStatus = updateEventsRegistry.undoRedoStatus()
        val updates = updateEventsRegistry.getUpdateEventList()
        val version = updateEventsRegistry.allBeforeThis

        updates.map { it.event }.forEach { event ->
            when (event) {
                is LocationUpdateWithId -> {
                    updatedShapes = updatedShapes.map { shape -> if (shape.id == event.diagramElementId) updateShapeLocation(shape, event) else shape }.toMutableList()
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.parentElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is BpmnShapeResizedAndMoved -> {
                    updatedShapes = updatedShapes.map { shape -> if (shape.id == event.diagramElementId) updateShapeLocationAndSize(shape, event) else shape }.toMutableList()
                }
                is NewWaypoints -> {
                    updatedEdges = updatedEdges.map { edge -> if (edge.id == event.edgeElementId) updateWaypointLocation(edge, event) else edge }.toMutableList()
                }
                is BpmnElementRemoved -> {
                    handleRemoved(event.bpmnElementId, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId, updatedElemPropertiesByStaticElementId)
                }
                is DiagramElementRemoved -> {
                    handleDiagramRemoved(event.elementId, updatedShapes, updatedEdges, updatedElementByDiagramId)
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
                    updatedProcessId = applyPropertyUpdate(updatedProcessId, event, updatedElemPropertiesByStaticElementId, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId)
                }
                is BpmnParentChanged -> {
                    for ((key, value) in updatedElementByStaticId) {
                        if (key != event.bpmnElementId) {
                            continue
                        }

                        if (event.propagateToXml) {
                            updatedElementByStaticId[key] = WithParentId(event.newParentId, value.element, event.newParentId)
                        } else {
                            updatedElementByStaticId[key] = WithParentId(event.newParentId, value.element, updatedElementByStaticId[key]?.parentIdForXml ?: event.newParentId)
                        }
                    }
                }
                is BooleanUiOnlyValueUpdatedEvent -> updateUiProperty(event, updatedElemUiOnlyPropertiesByStaticElementId)
                is IndexUiOnlyValueUpdatedEvent -> updateIndexProperty(event, updatedElemPropertiesByStaticElementId)
                is UiOnlyValueAddedEvent -> addUiOnlyProperty(event, updatedElemPropertiesByStaticElementId)
                is UiOnlyValueRemovedEvent -> removeUiOnlyProperty(event, updatedElemPropertiesByStaticElementId)
                else -> throw IllegalStateException("Can't handle event ${event.javaClass}")
            }
        }

        updatedElemPropertiesByStaticElementId.forEach {(elemId, props) ->
            props.forEach {type, value ->
                updatedPropertyWithElementByPropertyType.computeIfAbsent(type) { mutableMapOf() }[elemId] = value
            }
        }

        return CurrentState(
                updatedProcessId,
                updatedShapes,
                updatedEdges,
                updatedElementByDiagramId,
                updatedElementByStaticId,
                updatedElemPropertiesByStaticElementId,
                updatedPropertyWithElementByPropertyType,
                updatedElemUiOnlyPropertiesByStaticElementId,
                undoRedoStatus,
                version.toLong()
        )
    }

    private fun applyPropertyUpdate(
            processId: BpmnElementId,
            event: PropertyUpdateWithId,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>
    ) : BpmnElementId {
        if (event.property.elementUpdateChangesClass) {
            updateElementType(event, updatedElementByStaticId)
            return processId
        }

        if (null != event.newIdValue) {
            return updateId(processId, event.bpmnElementId, event.newIdValue!!, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId, updatedElemPropertiesByStaticElementId)
        }

        updateProperty(event, updatedElemPropertiesByStaticElementId)
        return processId
    }

    private fun updateElementType(event: PropertyUpdateWithId, updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>) {
        if (event.property != PropertyType.IS_TRANSACTIONAL_SUBPROCESS) {
            throw IllegalArgumentException("Can't change class for: ${event.property.name}}")
        }

        val mapper = Mappers.getMapper(MapTransactionalSubprocessToSubprocess::class.java)
        val withPrentElem = updatedElementByStaticId[event.bpmnElementId]!!
        when (val elem = withPrentElem.element) {
            is BpmnSubProcess -> {
                updatedElementByStaticId[event.bpmnElementId] = WithParentId(withPrentElem.parent, mapper.map(elem))

            }
            is BpmnTransactionalSubProcess -> {
                updatedElementByStaticId[event.bpmnElementId] = WithParentId(withPrentElem.parent, mapper.map(elem))
            }
            else -> throw IllegalStateException("Unexpected element: ${elem.javaClass.canonicalName}")

        }
    }

    private fun updateProperty(event: PropertyUpdateWithId, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: PropertyTable(mutableMapOf())
        if (null == event.propertyIndex) {
            updated[event.property] = Property(event.newValue)
        } else {
            val result = mutableListOf<Property>()
            var eventProcessed = false
            for (prop in updated.getAll(event.property)) {
                result += if (prop.index == event.propertyIndex && !eventProcessed) {
                    eventProcessed = true
                    Property(event.newValue, event.propertyIndex!!)
                } else {
                    prop
                }
            }

            if (!eventProcessed) {
                result += Property(event.newValue, event.propertyIndex!!)
            }
            updated[event.property] = result
        }

        updatedElemPropertiesByStaticElementId[event.bpmnElementId] = updated
    }

    private fun updateUiProperty(event: BooleanUiOnlyValueUpdatedEvent, updatedElemUiOnlyPropertiesByStaticElementId: MutableMap<BpmnElementId, Map<UiOnlyPropertyType, Property>>) {
        val updated = updatedElemUiOnlyPropertiesByStaticElementId[event.bpmnElementId]?.toMutableMap() ?: mutableMapOf()
        updated[event.property] = Property(event.newValue)
        updatedElemUiOnlyPropertiesByStaticElementId[event.bpmnElementId] = updated
    }

    private fun updateIndexProperty(event: IndexUiOnlyValueUpdatedEvent, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: PropertyTable(mutableMapOf())
        updated[event.property] = updated.getAll(event.property).map {
            if (it.index == event.propertyIndex || (null == it.index && event.propertyIndex.isEmpty())) {
                it.copy(index = event.newValue)
            } else it
        }.toMutableList()
        updatedElemPropertiesByStaticElementId[event.bpmnElementId] = updated
    }

    private fun addUiOnlyProperty(event: UiOnlyValueAddedEvent, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: PropertyTable(mutableMapOf())
        updated[event.property] = (updated.getAll(event.property)+ Property(event.newValue, event.propertyIndex!!)).toMutableList()
    }

    private fun removeUiOnlyProperty(event: UiOnlyValueRemovedEvent, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: PropertyTable(mutableMapOf())
        updated[event.property] = (updated.getAll(event.property).filter { it.index != event.propertyIndex }).toMutableList()
    }

    private fun handleDiagramRemoved(diagramId: DiagramElementId, updatedShapes: MutableList<ShapeElement>, updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>, updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>) {
        val shape = updatedShapes.find { it.id == diagramId }
        val edge = updatedEdges.find { it.id == diagramId }
        shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
        edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
        updatedElementByDiagramId.remove(diagramId)
    }

    private fun handleRemoved(
            elementId: BpmnElementId,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>
    ) {
        val shape = updatedShapes.find { it.bpmnElement == elementId }
        val edge = updatedEdges.find { it.bpmnElement == elementId }
        shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
        edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
        updatedElementByStaticId.remove(elementId)
        updatedElemPropertiesByStaticElementId.remove(elementId)
    }

    private fun updateId(
            processId: BpmnElementId,
            elementId: BpmnElementId,
            newElementId: BpmnElementId,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>
    ): BpmnElementId {
        val shape = updatedShapes.find { it.bpmnElement == elementId }
        val edge = updatedEdges.find { it.bpmnElement == elementId }
        shape?.let {
            updatedShapes.remove(it)
            updatedElementByDiagramId[it.id] = newElementId
            updatedShapes.add(it.copy(bpmnElement = newElementId))
        }
        edge?.let {
            updatedEdges.remove(it)
            updatedElementByDiagramId[it.id] = newElementId
            updatedEdges.add(it.updateBpmnElemId(newElementId))
        }
        val elemByBpmnIdUpdated = updatedElementByStaticId.remove(elementId)
        elemByBpmnIdUpdated?.let { updatedElementByStaticId[newElementId] = WithParentId(it.parent, it.updateBpmnElemId(newElementId)) }

        // Cascade ID update to children:
        updatedElementByStaticId.forEach { (elemId, elem) ->
            if (elem.parent == elementId) {
                updatedElementByStaticId[elemId] = WithParentId(newElementId, elem.element)
            }
        }
        val elemPropUpdated = updatedElemPropertiesByStaticElementId.remove(elementId) ?: PropertyTable(mutableMapOf())
        elemPropUpdated[PropertyType.ID] = Property(newElementId.id)
        updatedElemPropertiesByStaticElementId[newElementId] = elemPropUpdated

        if (elementId == processId) {
            updatedElementByDiagramId[CurrentState.processDiagramId(newElementId)] = newElementId
            return newElementId
        }

        return processId
    }

    private fun updateShapeLocationAndSize(elem: ShapeElement, update: BpmnShapeResizedAndMoved): ShapeElement {
        return elem.copyAndResize { update.transform(it) }
    }

    private fun updateShapeLocation(elem: ShapeElement, update: LocationUpdateWithId): ShapeElement {
        return elem.copyAndTranslate(update.dx, update.dy)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: LocationUpdateWithId): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, elem.waypoint.filter { it.physical }.map { if (it.id == update.diagramElementId ) it.copyAndTranslate(update.dx, update.dy) else it }, elem.epoch)
    }

    private fun updateWaypointLocation(elem: EdgeWithIdentifiableWaypoints, update: NewWaypoints): EdgeWithIdentifiableWaypoints {
        return EdgeElementState(elem, update.waypoints, update.epoch)
    }
}
