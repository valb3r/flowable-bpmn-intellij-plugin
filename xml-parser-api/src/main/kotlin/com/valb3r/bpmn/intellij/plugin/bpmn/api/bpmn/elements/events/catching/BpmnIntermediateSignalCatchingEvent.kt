package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnIntermediateSignalCatchingEvent(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?
) : WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}