package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnCollaboration(
    override val id: BpmnElementId,
    val name: String?,
    val participant: List<BpmnParticipant>?,
    val messageFlow: List<BpmnMessageFlow>?,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class BpmnParticipant(
    override val id: BpmnElementId,
    val name: String?,
    val processRef: String?,
    val documentation: String?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class BpmnMessageFlow(
    override val id: BpmnElementId,
    val name: String?,
    val documentation: String?,
    val sourceRef: String?,
    val targetRef: String?,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}
