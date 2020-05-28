package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*

@KotlinBuilder
data class BpmnProcess(
        val id: BpmnElementId,
        val name: String,
        val documentation: String?,
        val isExecutable: Boolean?,
        val startEvent: List<BpmnStartEvent>?,
        val callActivity: List<BpmnCallActivity>?,
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
        val sequenceFlow: List<BpmnSequenceFlow>?,
        val exclusiveGateway: List<BpmnExclusiveGateway>?,
        val endEvent: List<BpmnEndEvent>?
)