package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnLaneSet(
    override val id: BpmnElementId,
    val lanes: List<BpmnLane>?,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class BpmnLane(
    override val id: BpmnElementId,
    val name: String?,
    val documentation: String?,
    val flowNodeRef: List<BpmnFlowNodeRef>?,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class BpmnFlowNodeRef(val ref: String)
