package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

// TODO - move to some implementation module
data class BpmnProcessObject(val process: BpmnProcess, val diagram: List<DiagramElement>) {

    fun toView(factory: BpmnObjectFactory) : BpmnProcessObjectView {
        val elementByDiagramId = mutableMapOf<DiagramElementId, BpmnElementId>()
        val elementByStaticId = mutableMapOf<BpmnElementId, WithBpmnId>()
        val propertiesById = mutableMapOf<BpmnElementId, MutableMap<PropertyType, Property>>()

        process.body?.let { extractElementsFromBody(it, factory, elementByStaticId, propertiesById) }
        process.children?.forEach { (_, body) -> extractElementsFromBody(body, factory, elementByStaticId, propertiesById)}

        diagram.firstOrNull()
                ?.bpmnPlane
                ?.bpmnEdge
                ?.filter { null != it.bpmnElement }
                ?.forEach { elementByDiagramId[it.id] = it.bpmnElement!! }

        diagram.firstOrNull()
                ?.bpmnPlane
                ?.bpmnShape
                ?.forEach { elementByDiagramId[it.id] = it.bpmnElement }

        return BpmnProcessObjectView(
                process.id,
                elementByDiagramId,
                elementByStaticId,
                propertiesById,
                diagram
        )
    }

    private fun extractElementsFromBody(
            body: BpmnProcessBody,
            factory: BpmnObjectFactory, 
            elementByStaticId: MutableMap<BpmnElementId, WithBpmnId>, 
            propertiesById: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        // Events
        // Start
        body.startEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.conditionalStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.errorStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.escalationStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.messageStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.signalStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.timerStartEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        // End
        body.endEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.errorEndEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.escalationEndEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.cancelEndEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.terminateEndEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        // Boundary
        body.boundaryEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryCancelEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryCompensationEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryConditionalEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryErrorEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryEscalationEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryMessageEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundarySignalEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.boundaryTimerEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        // Catching
        body.intermediateTimerCatchingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.intermediateMessageCatchingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.intermediateSignalCatchingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.intermediateConditionalCatchingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        // Throwing
        body.intermediateNoneThrowingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.intermediateSignalThrowingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.intermediateEscalationThrowingEvent?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }

        // Service-task alike
        body.userTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.scriptTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.serviceTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.businessRuleTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.receiveTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.camelTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.httpTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.muleTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.decisionTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.shellTask?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }

        // Sub-process alike
        body.callActivity?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.subProcess?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.transaction?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.adHocSubProcess?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }

        // Gateways
        body.exclusiveGateway?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.parallelGateway?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.inclusiveGateway?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
        body.eventBasedGateway?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }

        // Linking elements
        body.sequenceFlow?.forEach { fillFor(factory, it, elementByStaticId, propertiesById) }
    }

    private fun fillFor(
            factory: BpmnObjectFactory,
            activity: WithBpmnId,
            elementById: MutableMap<BpmnElementId, WithBpmnId>,
            propertiesByElemType: MutableMap<BpmnElementId, MutableMap<PropertyType, Property>>) {
        elementById[activity.id] = activity
        propertiesByElemType[activity.id] = factory.propertiesOf(activity).toMutableMap()
    }

}

data class BpmnProcessObjectView(
        val processId: BpmnElementId,
        val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        val elementByStaticId: Map<BpmnElementId, WithBpmnId>,
        val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>,
        val diagram: List<DiagramElement>
)