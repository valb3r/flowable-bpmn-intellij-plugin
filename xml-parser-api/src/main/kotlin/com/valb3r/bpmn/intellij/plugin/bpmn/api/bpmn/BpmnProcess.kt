package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes.BpmnLaneSet
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*

data class BpmnProcessBody(

        // Events
        // Start
        val startEvent: List<BpmnStartEvent>?,
        val timerStartEvent: List<BpmnStartTimerEvent>?,
        val signalStartEvent: List<BpmnStartSignalEvent>?,
        val messageStartEvent: List<BpmnStartMessageEvent>?,
        val errorStartEvent: List<BpmnStartErrorEvent>?,
        val escalationStartEvent: List<BpmnStartEscalationEvent>?,
        val conditionalStartEvent: List<BpmnStartConditionalEvent>?,
        // End
        val endEvent: List<BpmnEndEvent>?,
        val errorEndEvent: List<BpmnEndErrorEvent>?,
        val escalationEndEvent: List<BpmnEndEscalationEvent>?,
        val cancelEndEvent: List<BpmnEndCancelEvent>?,
        val terminateEndEvent: List<BpmnEndTerminateEvent>?,
        // Boundary
        val boundaryEvent: List<BpmnBoundaryEvent>?,
        val boundaryCancelEvent: List<BpmnBoundaryCancelEvent>?,
        val boundaryCompensationEvent: List<BpmnBoundaryCompensationEvent>?,
        val boundaryConditionalEvent: List<BpmnBoundaryConditionalEvent>?,
        val boundaryErrorEvent: List<BpmnBoundaryErrorEvent>?,
        val boundaryEscalationEvent: List<BpmnBoundaryEscalationEvent>?,
        val boundaryMessageEvent: List<BpmnBoundaryMessageEvent>?,
        val boundarySignalEvent: List<BpmnBoundarySignalEvent>?,
        val boundaryTimerEvent: List<BpmnBoundaryTimerEvent>?,
        // Catching
        val intermediateCatchEvent: List<BpmnIntermediateCatchingEvent>?, // generic mapping point should be empty when processed
        val intermediateTimerCatchingEvent: List<BpmnIntermediateTimerCatchingEvent>?,
        val intermediateMessageCatchingEvent: List<BpmnIntermediateMessageCatchingEvent>?,
        val intermediateSignalCatchingEvent: List<BpmnIntermediateSignalCatchingEvent>?,
        val intermediateConditionalCatchingEvent: List<BpmnIntermediateConditionalCatchingEvent>?,
        val intermediateLinkCatchingEvent: List<BpmnIntermediateLinkCatchingEvent>?,
        // Throwing
        val intermediateThrowEvent: List<BpmnIntermediateThrowingEvent>?, // generic mapping point should be empty when processed
        val intermediateNoneThrowingEvent: List<BpmnIntermediateNoneThrowingEvent>?,
        val intermediateSignalThrowingEvent: List<BpmnIntermediateSignalThrowingEvent>?,
        val intermediateEscalationThrowingEvent: List<BpmnIntermediateEscalationThrowingEvent>?,

        // Service-task alike
        val task: List<BpmnTask>?,
        val userTask: List<BpmnUserTask>?,
        val scriptTask: List<BpmnScriptTask>?,
        val serviceTask: List<BpmnServiceTask>?,
        val businessRuleTask: List<BpmnBusinessRuleTask>?,
        val manualTask: List<BpmnManualTask>?,
        val sendTask: List<BpmnSendTask>?,
        val receiveTask: List<BpmnReceiveTask>?,

        // Customizations of ServiceTask
        val camelTask: List<BpmnCamelTask>?,
        val httpTask: List<BpmnHttpTask>?,
        val mailTask: List<BpmnMailTask>?,
        val muleTask: List<BpmnMuleTask>?,
        val decisionTask: List<BpmnDecisionTask>?,
        val shellTask: List<BpmnShellTask>?,
        // end customizations

        // Sub-process alike
        val callActivity: List<BpmnCallActivity>?,
        // These elems are to be flattened as they recurse into BpmnProcess themselves
        val subProcess: List<BpmnSubProcess>?,
        val eventSubProcess: List<BpmnEventSubprocess>?,
        val transaction: List<BpmnTransactionalSubProcess>?,
        val adHocSubProcess: List<BpmnAdHocSubProcess>?,
        val collapsedSubProcess: List<BpmnCollapsedSubprocess>?,
        val collapsedTransaction: List<BpmnTransactionCollapsedSubprocess>?,

        // Gateways
        val exclusiveGateway: List<BpmnExclusiveGateway>?,
        val parallelGateway: List<BpmnParallelGateway>?,
        val inclusiveGateway: List<BpmnInclusiveGateway>?,
        val eventBasedGateway: List<BpmnEventGateway>?,
        val complexGateway: List<BpmnComplexGateway>?,

        // Linking elements
        val sequenceFlow: List<BpmnSequenceFlow>?
)

data class BpmnProcess(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val isExecutable: Boolean?,

        // Process body
        val body: BpmnProcessBody?,

        // Process children (i.e. subProcess)
        // Using flat data approach as CycleAvoidingMappingContext seem to be an issue with @KotlinBuilder
        // Child BPMN processes in rendering order, flattened, so that i.e. subProcess is flat simple object (id, docs, ...)
        // and not recursion object
        val children: Map<BpmnElementId, BpmnProcessBody>?,
        val laneSets: List<BpmnLaneSet>?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}
