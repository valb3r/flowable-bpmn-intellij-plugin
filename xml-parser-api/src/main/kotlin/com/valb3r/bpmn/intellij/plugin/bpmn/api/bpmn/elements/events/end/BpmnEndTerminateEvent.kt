package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnEndTerminateEvent(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

