package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes

data class BpmnLaneSet(
    val id: String,
    val lanes: List<BpmnLane>?,
)

data class BpmnLane(
    val id: String,
    val name: String?,
    val documentation: String?,
    val flowNodeRef: List<BpmnFlowNodeRef>?,
)

data class BpmnFlowNodeRef(val ref: String)
