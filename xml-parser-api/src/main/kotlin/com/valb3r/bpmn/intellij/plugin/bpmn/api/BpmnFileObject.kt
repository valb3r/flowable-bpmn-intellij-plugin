package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnCollaboration
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes.BpmnLaneSet
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

// TODO - move to some implementation module
data class BpmnFileObject(val processes: List<BpmnProcess>, val collaborations: List<BpmnCollaboration>, val diagram: List<DiagramElement>) {

    fun toView(factory: BpmnObjectFactory) : BpmnFileView {
        val mappedCollaborations = mapCollaborations(factory)
        val rootProcessOrCollaborationId = mappedCollaborations.firstOrNull()?.collaborationId ?: BpmnElementId("")
        val collaborationProcessRoots = collaborations.flatMap { it.participant ?: emptyList() }
            .filter { null != it.processRef }
            .groupBy { it.processRef!! }
            .mapValues { entry -> entry.value.first().id }
        val mappedProcesses = mapProcesses(factory, rootProcessOrCollaborationId, collaborationProcessRoots)

        return BpmnFileView(rootProcessOrCollaborationId, mappedProcesses, mappedCollaborations)
    }

    private fun mapCollaborations(factory: BpmnObjectFactory): List<BpmnCollaborationView> {
        val mappedCollaborations = mutableListOf<BpmnCollaborationView>()
        for (collaboration in collaborations) {
            val elementByStaticId = mutableMapOf<BpmnElementId, WithParentId>()
            val propertiesById = mutableMapOf<BpmnElementId, PropertyTable>()

            fillFor(BpmnElementId(""), factory, collaboration, elementByStaticId, propertiesById)

            collaboration.participant?.forEach { fillFor(collaboration.id, factory, it, elementByStaticId, propertiesById)}
            collaboration.messageFlow?.forEach { fillFor(collaboration.id, factory, it, elementByStaticId, propertiesById)}

            mappedCollaborations += BpmnCollaborationView(
                collaboration.id,
                elementByStaticId,
                propertiesById
            )
        }

        return mappedCollaborations
    }

    private fun mapProcesses(factory: BpmnObjectFactory, rootId: BpmnElementId, collaborationByProcess: Map<String, BpmnElementId>): List<BpmnProcessObjectView> {
        val mappedProcesses = mutableListOf<BpmnProcessObjectView>()
        val allElementsByDiagramId = mutableMapOf<DiagramElementId, BpmnElementId>()
        for (process in processes) {
            val elementByStaticId = mutableMapOf<BpmnElementId, WithParentId>()
            val propertiesById = mutableMapOf<BpmnElementId, PropertyTable>()

            fillFor(collaborationByProcess.getOrDefault(process.id.id, rootId), factory, process, elementByStaticId, propertiesById)
            allElementsByDiagramId[DiagramElementId(process.id.id)] = process.id

            // 1st pass
            process.body?.let { extractElementsFromBody(process.id, it, factory, elementByStaticId, propertiesById) }
            process.children?.forEach { (id, body) -> extractElementsFromBody(id, body, factory, elementByStaticId, propertiesById) }
            process.laneSets?.forEach { extractElementsFromLanes(process.id, it, factory, elementByStaticId, propertiesById) }
            // 2nd pass
            process.body?.let { reassignParentsBasedOnTargetRef(process.id, it, factory, elementByStaticId, propertiesById) }
            process.children?.forEach { (id, body) -> reassignParentsBasedOnTargetRef(id, body, factory, elementByStaticId, propertiesById) }
            diagram.flatMap { it.bpmnPlane.bpmnEdge ?: emptyList() }
                .filter { null != it.bpmnElement }
                .forEach { allElementsByDiagramId[it.id] = it.bpmnElement!! }

            diagram.flatMap { it.bpmnPlane.bpmnShape ?: emptyList() }
                .forEach { allElementsByDiagramId[it.id] = it.bpmnElement }

            mappedProcesses += BpmnProcessObjectView(
                process.id,
                allElementsByDiagramId,
                elementByStaticId,
                propertiesById,
                diagram
            )
        }

        return mappedProcesses
    }

    private fun extractElementsFromBody(
            parentId: BpmnElementId,
            body: BpmnProcessBody,
            factory: BpmnObjectFactory, 
            elementByStaticId: MutableMap<BpmnElementId, WithParentId>, 
            propertiesById: MutableMap<BpmnElementId, PropertyTable>) {
        // Events
        // Start
        body.startEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.conditionalStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.errorStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.escalationStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.messageStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.signalStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.timerStartEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        // End
        body.endEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.errorEndEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.escalationEndEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.cancelEndEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.terminateEndEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        // Boundary
        body.boundaryEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryCancelEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryCompensationEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryConditionalEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryErrorEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryEscalationEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryMessageEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundarySignalEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryTimerEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        // Catching
        body.intermediateTimerCatchingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateMessageCatchingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateSignalCatchingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateConditionalCatchingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateLinkCatchingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        // Throwing
        body.intermediateNoneThrowingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateSignalThrowingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.intermediateEscalationThrowingEvent?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }

        // Service-task alike
        body.task?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.userTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.scriptTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.serviceTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.businessRuleTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.manualTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.sendTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.receiveTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.camelTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.httpTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.mailTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.muleTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.decisionTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.shellTask?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }

        // Sub-process alike
        body.callActivity?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.subProcess?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.eventSubProcess?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.transaction?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.adHocSubProcess?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.collapsedTransaction?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.collapsedSubProcess?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }

        // Gateways
        body.exclusiveGateway?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.parallelGateway?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.inclusiveGateway?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.eventBasedGateway?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
        body.complexGateway?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }

        // Linking elements
        body.sequenceFlow?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
    }

    private fun extractElementsFromLanes(
        parentId: BpmnElementId,
        laneSet: BpmnLaneSet,
        factory: BpmnObjectFactory,
        elementByStaticId: MutableMap<BpmnElementId, WithParentId>,
        propertiesById: MutableMap<BpmnElementId, PropertyTable>) {
        laneSet.lanes?.forEach { fillFor(parentId, factory, it, elementByStaticId, propertiesById) }
    }

    private fun reassignParentsBasedOnTargetRef(
            parentId: BpmnElementId,
            body: BpmnProcessBody,
            factory: BpmnObjectFactory,
            elementByStaticId: MutableMap<BpmnElementId, WithParentId>,
            propertiesById: MutableMap<BpmnElementId, PropertyTable>) {
        // Boundary
        body.boundaryEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryCancelEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryCompensationEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryConditionalEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryErrorEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryEscalationEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryMessageEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundarySignalEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
        body.boundaryTimerEvent?.forEach { fillForTargetRefParent(parentId, factory, it, elementByStaticId, propertiesById) }
    }

    private fun fillFor(
            parentId: BpmnElementId,
            factory: BpmnObjectFactory,
            activity: WithBpmnId,
            elementById: MutableMap<BpmnElementId, WithParentId>,
            propertiesByElemType: MutableMap<BpmnElementId, PropertyTable>) {
        elementById[activity.id] = WithParentId(parentId, activity)
        propertiesByElemType[activity.id] = factory.propertiesOf(activity)
    }

    private fun fillForTargetRefParent(
            defaultParentId: BpmnElementId,
            factory: BpmnObjectFactory,
            activity: WithBpmnId,
            elementById: MutableMap<BpmnElementId, WithParentId>,
            propertiesByElemType: MutableMap<BpmnElementId, PropertyTable>) {
        propertiesByElemType[activity.id] = factory.propertiesOf(activity)
        var parentId = defaultParentId
        val referencedId = propertiesByElemType[activity.id]?.get(PropertyType.ATTACHED_TO_REF)?.value
        referencedId?.let {referenced ->
            val computedParent = elementById[BpmnElementId(referenced as String)]
            computedParent?.let { parentId = it.id }
        }

        elementById[activity.id] = WithParentId(parentId, activity, defaultParentId)
    }
}

data class BpmnFileView(
    val primaryProcessId: BpmnElementId,
    val processes: List<BpmnProcessObjectView>,
    val collaborations: List<BpmnCollaborationView>
)

data class BpmnCollaborationView(
    val collaborationId: BpmnElementId,
    val collaborationElementByStaticId: Map<BpmnElementId, WithParentId>,
    val collaborationElemPropertiesByElementId: Map<BpmnElementId, PropertyTable>
)

data class BpmnProcessObjectView(
    val processId: BpmnElementId,
    val allElementsByDiagramId: Map<DiagramElementId, BpmnElementId>,
    val processElementByStaticId: Map<BpmnElementId, WithParentId>,
    val processElemPropertiesByElementId: Map<BpmnElementId, PropertyTable>,
    val diagram: List<DiagramElement>
)

data class PropertyTable(private val properties: MutableMap<PropertyType, MutableList<Property>>) {

    val keys get() = properties.keys

    operator fun get(type: PropertyType): Property? {
        return properties[type]?.getOrNull(0)
    }

    operator fun set(type: PropertyType, value: Property) {
        properties[type] = mutableListOf(value)
    }

    operator fun set(type: PropertyType, values: MutableList<Property>) {
        properties[type] = values
    }

    fun add(type: PropertyType, value: Property) {
        properties.computeIfAbsent(type) { mutableListOf()}.add(value)
    }

    fun forEach(exec: (key: PropertyType, value: Property) -> Unit) {
        properties.forEach {(k, vals) -> vals.forEach {exec(k, it)}}
    }

    fun filter(exec: (key: PropertyType, value: Property) -> Boolean): List<Pair<PropertyType, Property>> {
        return properties.flatMap { it.value.map { v -> Pair(it.key, v) } }.filter {(k, v) -> exec(k, v)}
    }

    fun getAll(type: PropertyType): List<Property> {
        return properties[type] ?: emptyList()
    }

    fun getAllInitialized(type: PropertyType): List<Property> {
        return properties[type]?.filter { null == type.group || null != it.index } ?: emptyList()
    }

    fun view(): Map<PropertyType, List<Property>> {
        return properties
    }

    fun copy(): PropertyTable {
        return PropertyTable(properties.mapValues { it.value.toMutableList() }.toMutableMap())
    }
}
