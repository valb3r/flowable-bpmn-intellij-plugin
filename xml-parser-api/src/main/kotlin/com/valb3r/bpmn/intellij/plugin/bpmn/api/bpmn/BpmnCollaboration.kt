package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

data class BpmnCollaboration(
    val id: String,
    val name: String?,
    val participant: List<BpmnParticipant>?,
    val messageFlow: List<BpmnMessageFlow>?,
)

data class BpmnParticipant(
    val id: String,
    val name: String?,
    val processRef: String,
    val documentation: String?
)

data class BpmnMessageFlow(
    val id: String,
    val name: String?,
    val documentation: String?,
    val sourceRef: String,
    val targetRef: String,
)
