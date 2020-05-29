package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*

@KotlinBuilder
data class BpmnProcess(
        val id: BpmnElementId,
        val name: String,
        val documentation: String?,
        val isExecutable: Boolean?,

        // Events
        val startEvent: List<BpmnStartEvent>?,
        val endEvent: List<BpmnEndEvent>?,

        // Service-task alike
        val userTask: List<BpmnUserTask>?,
        val scriptTask: List<BpmnScriptTask>?,
        val serviceTask: List<BpmnServiceTask>?,
        val businessRuleTask: List<BpmnBusinessRuleTask>?,
        val receiveTask: List<BpmnReceiveTask>?,

        // Customizations of ServiceTask
        val camelTask: List<BpmnCamelTask>?,
        val httpTask: List<BpmnHttpTask>?,
        val muleTask: List<BpmnMuleTask>?,
        val decisionTask: List<BpmnDecisionTask>?,
        val shellTask: List<BpmnShellTask>?,
        // end customizations

        // Sub-process alike
        val callActivity: List<BpmnCallActivity>?,
        val subProcess: List<BpmnSubProcess>?,
        val transaction: List<BpmnTransactionalSubProcess>?,
        val adHocSubProcess: List<BpmnAdHocSubProcess>?,

        // Gateways
        val exclusiveGateway: List<BpmnExclusiveGateway>?,
        val parallelGateway: List<BpmnParallelGateway>?,
        val inclusiveGateway: List<BpmnInclusiveGateway>?,
        val eventBasedGateway: List<BpmnEventGateway>?,

        // Linking elements
        val sequenceFlow: List<BpmnSequenceFlow>?
)