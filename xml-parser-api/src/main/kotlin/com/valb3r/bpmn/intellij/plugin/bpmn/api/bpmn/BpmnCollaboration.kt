package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnCollaboration(
    override val id: BpmnElementId,
    ): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}
