package com.valb3r.bpmn.intellij.plugin.core.state

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
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
    val primaryProcessId: BpmnElementId,
    val processes: Set<BpmnElementId>,
    val shapes: List<ShapeElement>,
    val edges: List<EdgeWithIdentifiableWaypoints>,
    val elementsByDiagramId: Map<DiagramElementId, BpmnElementId>,
    val elementByBpmnId: Map<BpmnElementId, WithParentId>,
    val elemPropertiesByStaticElementId: Map<BpmnElementId, PropertyTable>,
    val propertyWithElementByPropertyType: Map<PropertyType, Map<BpmnElementId, Property>>,
    val elemUiOnlyPropertiesByStaticElementId: Map<BpmnElementId, Map<UiOnlyPropertyType, Property>>,
    val undoRedo: Set<ProcessModelUpdateEvents.UndoRedo>,
    val version: Long,
    val diagramByElementId: Map<BpmnElementId, DiagramElementId> = elementsByDiagramId.map { Pair(it.value, it.key) }.toMap(),
) {
    fun primaryProcessDiagramId(): DiagramElementId {
        return primaryProcessDiagramId(primaryProcessId)
    }

    companion object {
        fun primaryProcessDiagramId(processId: BpmnElementId): DiagramElementId {
            return DiagramElementId(processId.id)
        }
    }
}

private val ZERO_STATE = CurrentState(BpmnElementId(""), emptySet(), emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), 0L)

// Global singleton
class CurrentStateProvider(private val project: Project) {
    private val mapper = Mappers.getMapper(MapTransactionalSubprocessToSubprocess::class.java)

    private var fileState = ZERO_STATE.copy()
    private var currentState = ZERO_STATE.copy()
    private val version = AtomicLong(0L)

    fun resetStateTo(fileContent: String, view: BpmnFileView) {
        version.set(0L)
        fileState = CurrentState(
                view.primaryProcessId,
                view.processes.map { it.processId }.toSet(),
                view.processes.flatMap { proc -> proc.diagram.flatMap { it.bpmnPlane.bpmnShape ?: emptyList() } },
                view.processes.flatMap { proc -> proc.diagram.flatMap { it.bpmnPlane.bpmnEdge ?: emptyList() }.map { EdgeElementState(it) } },
                extractDiagramElementToBpmnIds(view),
                extractBpmnElements(view),
                extractProperties(view),
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

    private fun extractProperties(view: BpmnFileView) =
        view.processes.flatMap { proc -> proc.processElemPropertiesByElementId.entries }.groupBy { it.key }.mapValues { it.value.first().value } +
                view.collaborations.flatMap { coll -> coll.collaborationElemPropertiesByElementId.entries }.groupBy { it.key }.mapValues { it.value.first().value }

    private fun extractBpmnElements(view: BpmnFileView) =
        view.processes.flatMap { proc -> proc.processElementByStaticId.entries }.groupBy { it.key }.mapValues { it.value.first().value } +
                view.collaborations.flatMap { coll -> coll.collaborationElementByStaticId.entries }.groupBy { it.key }.mapValues { it.value.first().value }

    private fun extractDiagramElementToBpmnIds(view: BpmnFileView) =
        view.processes.flatMap { proc -> proc.allElementsByDiagramId.entries }.groupBy { it.key }.mapValues { it.value.first().value }

    private fun handleUpdates(state: CurrentState): CurrentState {
        var updatedShapes = state.shapes.toMutableList()
        var updatedEdges = state.edges.toMutableList()
        val updatedElementByDiagramId = state.elementsByDiagramId.toMutableMap()
        val updatedElementByStaticId = state.elementByBpmnId.toMutableMap()
        val updatedElemPropertiesByStaticElementId = state.elemPropertiesByStaticElementId.mapValues { it.value.copy() }.toMutableMap()
        val updatedPropertyWithElementByPropertyType = mutableMapOf<PropertyType, MutableMap<BpmnElementId, Property>>()
        val updatedElemUiOnlyPropertiesByStaticElementId = state.elemUiOnlyPropertiesByStaticElementId.toMutableMap()
        var updatedProcessId = state.primaryProcessId
        val updatedProcessSet = state.processes.toMutableSet()
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
                    handleRemoved(event.bpmnElementId, updatedProcessSet, updatedShapes, updatedEdges, updatedElementByDiagramId, updatedElementByStaticId, updatedElemPropertiesByStaticElementId)
                }
                is BpmnElementChange -> {
                    handleChangeType(event.elementId, event.newBpmnElement, updatedElementByStaticId, event.props, updatedElemPropertiesByStaticElementId)
                }
                is DiagramElementRemoved -> {
                    handleDiagramRemoved(event.elementId, updatedShapes, updatedEdges, updatedElementByDiagramId)
                }
                is BpmnProcessObjectAdded -> {
                    updatedProcessSet.add(event.bpmnObject.id)
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props.copy()
                }
                is BpmnShapeObjectAdded -> {
                    updatedShapes.add(event.shape)
                    updatedElementByDiagramId[event.shape.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props.copy()
                }
                is BpmnEdgeObjectAdded -> {
                    updatedEdges.add(event.edge)
                    updatedElementByDiagramId[event.edge.id] = event.bpmnObject.id
                    updatedElementByStaticId[event.bpmnObject.id] = event.bpmnObject
                    updatedElemPropertiesByStaticElementId[event.bpmnObject.id] = event.props.copy()
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
                updatedProcessSet,
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
//                if (it.index!![0] == event.propertyIndex[0] || (null == it.index && event.propertyIndex.isEmpty())) {
                    it.copy(index = event.newValue)
                } else it
            }.toMutableList()
            updatedElemPropertiesByStaticElementId[event.bpmnElementId] = updated
    }

    private fun addUiOnlyProperty(event: UiOnlyValueAddedEvent, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: return
        updated[event.property] = (updated.getAllInitialized(event.property) + Property(event.newValue, event.propertyIndex!!)).toSet().toMutableList()
    }

    private fun removeUiOnlyProperty(event:  UiOnlyValueRemovedEvent, updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>) {
        val updated = updatedElemPropertiesByStaticElementId[event.bpmnElementId] ?: return
        val preparedProperty =
            (updated.getAll(event.property).filter { it.index != event.propertyIndex }).toSet().toMutableList()
            if (preparedProperty.size > 0) {
            updated[event.property] = preparedProperty
        } else {
            if (event.property.group!!.size > 1) {
                val prepareIndex = event.propertyIndex!!.toMutableList()
                prepareIndex[prepareIndex.size - 1] = ""
                updated[event.property] = mutableListOf(Property(null, prepareIndex.toList()))
            } else {
                updated[event.property] = mutableListOf(Property(null, null))
            }
        }
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
            processes: MutableSet<BpmnElementId>,
            updatedShapes: MutableList<ShapeElement>,
            updatedEdges: MutableList<EdgeWithIdentifiableWaypoints>,
            updatedElementByDiagramId: MutableMap<DiagramElementId, BpmnElementId>,
            updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>,
            updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>
    ) {
        processes.remove(elementId)
        val shape = updatedShapes.find { it.bpmnElement == elementId }
        val edge = updatedEdges.find { it.bpmnElement == elementId }
        shape?.let { updatedElementByDiagramId.remove(it.id); updatedShapes.remove(it) }
        edge?.let { updatedElementByDiagramId.remove(it.id); updatedEdges.remove(it) }
        updatedElementByStaticId.remove(elementId)
        updatedElemPropertiesByStaticElementId.remove(elementId)
    }

    private fun handleChangeType(
        elementId: BpmnElementId,
        updateElement: WithBpmnId,
        updatedElementByStaticId: MutableMap<BpmnElementId, WithParentId>,
        propertyTable: PropertyTable,
        updatedElemPropertiesByStaticElementId: MutableMap<BpmnElementId, PropertyTable>
    ){
        val updateWithParentId = updatedElementByStaticId[elementId]!!.copy(element = updateElement)
        updatedElementByStaticId[elementId] = updateWithParentId
        updatedElemPropertiesByStaticElementId[elementId] = propertyTable
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
            updatedElementByDiagramId[CurrentState.primaryProcessDiagramId(newElementId)] = newElementId
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
