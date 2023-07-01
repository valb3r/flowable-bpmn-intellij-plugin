package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

// TODO - move to some implementation module
data class BpmnProcessObject(val process: BpmnProcess, val diagram: List<DiagramElement>) {

    fun toView(factory: BpmnObjectFactory) : BpmnProcessObjectView {
        val elementByDiagramId = mutableMapOf<DiagramElementId, BpmnElementId>()
        val elementByStaticId = mutableMapOf<BpmnElementId, WithParentId>()
        val propertiesById = mutableMapOf<BpmnElementId, PropertyTable>()

        fillFor(BpmnElementId(""), factory, process, elementByStaticId, propertiesById)
        elementByDiagramId[DiagramElementId(process.id.id)] = process.id

        val fillForElementProps = {id: BpmnElementId, elem: WithBpmnId -> fillFor(id, factory, elem, elementByStaticId, propertiesById)}
        val fillForExternalElementProps = { _: BpmnElementId, elem: WithBpmnId -> fillForExternal(elem, elementByStaticId, propertiesById)}
        // 1st pass
        process.body?.let { extractElementsFromBody(process.id, it, fillForElementProps) }
        process.children?.forEach { (id, body) -> extractElementsFromBody(id, body, fillForElementProps)}
        // 2nd pass
        process.body?.let { reassignParentsBasedOnTargetRef(process.id, it, factory, elementByStaticId, propertiesById) }
        process.children?.forEach { (id, body) -> reassignParentsBasedOnTargetRef(id, body, factory, elementByStaticId, propertiesById)}
        // 3rd pass - Deal with external properties:
        process.body?.let { extractElementsFromBody(process.id, it, fillForExternalElementProps) }
        process.children?.forEach { (id, body) -> extractElementsFromBody(id, body, fillForExternalElementProps)}

        diagram.flatMap { it.bpmnPlane.bpmnEdge ?: emptyList()}
                .filter { null != it.bpmnElement }
                .forEach { elementByDiagramId[it.id] = it.bpmnElement!! }

        diagram.flatMap { it.bpmnPlane.bpmnShape ?: emptyList() }
                .forEach { elementByDiagramId[it.id] = it.bpmnElement }

        return BpmnProcessObjectView(
                process.id,
                elementByDiagramId,
                elementByStaticId,
                propertiesById,
                diagram
        )
    }

    private fun extractElementsFromBody(
        parentId: BpmnElementId,
        body: BpmnProcessBody,
        fillFor: (BpmnElementId, WithBpmnId) -> Unit
    ) {
        // Events
        // Start
        body.startEvent?.forEach { fillFor(parentId, it) }
        body.conditionalStartEvent?.forEach { fillFor(parentId, it) }
        body.errorStartEvent?.forEach { fillFor(parentId, it) }
        body.escalationStartEvent?.forEach { fillFor(parentId, it) }
        body.messageStartEvent?.forEach { fillFor(parentId, it) }
        body.signalStartEvent?.forEach { fillFor(parentId, it) }
        body.timerStartEvent?.forEach { fillFor(parentId, it) }
        // End
        body.endEvent?.forEach { fillFor(parentId, it) }
        body.errorEndEvent?.forEach { fillFor(parentId, it) }
        body.escalationEndEvent?.forEach { fillFor(parentId, it) }
        body.cancelEndEvent?.forEach { fillFor(parentId, it) }
        body.terminateEndEvent?.forEach { fillFor(parentId, it) }
        // Boundary
        body.boundaryEvent?.forEach { fillFor(parentId, it) }
        body.boundaryCancelEvent?.forEach { fillFor(parentId, it) }
        body.boundaryCompensationEvent?.forEach { fillFor(parentId, it) }
        body.boundaryConditionalEvent?.forEach { fillFor(parentId, it) }
        body.boundaryErrorEvent?.forEach { fillFor(parentId, it) }
        body.boundaryEscalationEvent?.forEach { fillFor(parentId, it) }
        body.boundaryMessageEvent?.forEach { fillFor(parentId, it) }
        body.boundarySignalEvent?.forEach { fillFor(parentId, it) }
        body.boundaryTimerEvent?.forEach { fillFor(parentId, it) }
        // Catching
        body.intermediateTimerCatchingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateMessageCatchingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateSignalCatchingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateConditionalCatchingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateLinkCatchingEvent?.forEach { fillFor(parentId, it) }
        // Throwing
        body.intermediateNoneThrowingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateSignalThrowingEvent?.forEach { fillFor(parentId, it) }
        body.intermediateEscalationThrowingEvent?.forEach { fillFor(parentId, it) }

        // Service-task alike
        body.task?.forEach { fillFor(parentId, it) }
        body.userTask?.forEach { fillFor(parentId, it) }
        body.scriptTask?.forEach { fillFor(parentId, it) }
        body.serviceTask?.forEach { fillFor(parentId, it) }
        body.businessRuleTask?.forEach { fillFor(parentId, it) }
        body.manualTask?.forEach { fillFor(parentId, it) }
        body.sendTask?.forEach { fillFor(parentId, it) }
        body.receiveTask?.forEach { fillFor(parentId, it) }
        body.camelTask?.forEach { fillFor(parentId, it) }
        body.sendEventTask?.forEach { fillFor(parentId, it) }
        body.httpTask?.forEach { fillFor(parentId, it) }
        body.externalTask?.forEach { fillFor(parentId, it) }
        body.mailTask?.forEach { fillFor(parentId, it) }
        body.muleTask?.forEach { fillFor(parentId, it) }
        body.decisionTask?.forEach { fillFor(parentId, it) }
        body.shellTask?.forEach { fillFor(parentId, it) }

        // Sub-process alike
        body.callActivity?.forEach { fillFor(parentId, it) }
        body.subProcess?.forEach { fillFor(parentId, it) }
        body.eventSubProcess?.forEach { fillFor(parentId, it) }
        body.transaction?.forEach { fillFor(parentId, it) }
        body.adHocSubProcess?.forEach { fillFor(parentId, it) }
        body.collapsedTransaction?.forEach { fillFor(parentId, it) }
        body.collapsedSubProcess?.forEach { fillFor(parentId, it) }

        // Gateways
        body.exclusiveGateway?.forEach { fillFor(parentId, it) }
        body.parallelGateway?.forEach { fillFor(parentId, it) }
        body.inclusiveGateway?.forEach { fillFor(parentId, it) }
        body.eventBasedGateway?.forEach { fillFor(parentId, it) }
        body.complexGateway?.forEach { fillFor(parentId, it) }

        // Linking elements
        body.sequenceFlow?.forEach { fillFor(parentId, it) }
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
        element: WithBpmnId,
        elementById: MutableMap<BpmnElementId, WithParentId>,
        propertiesByElemId: MutableMap<BpmnElementId, PropertyTable>) {

        elementById[element.id] = WithParentId(parentId, element)
        propertiesByElemId[element.id] = factory.propertiesOf(element)
    }

    private fun fillForExternal(
        element: WithBpmnId,
        elementById: MutableMap<BpmnElementId, WithParentId>,
        propertiesByElemId: MutableMap<BpmnElementId, PropertyTable>) {
        val externalProperties = PropertyType.values().filter {
            it.isUsedOnlyBy.contains(element::class) && (it.externalProperty?.isPresent(elementById, propertiesByElemId, propertiesByElemId[element.id]!!) ?: false)
        }
        println()
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

data class BpmnProcessObjectView(
        val processId: BpmnElementId,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByStaticId: Map<BpmnElementId, WithParentId>,
        val elemPropertiesByElementId: Map<BpmnElementId, PropertyTable>,
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
