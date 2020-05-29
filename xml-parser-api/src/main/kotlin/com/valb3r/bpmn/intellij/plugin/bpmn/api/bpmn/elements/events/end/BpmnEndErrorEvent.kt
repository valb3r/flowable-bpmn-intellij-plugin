package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

@KotlinBuilder
data class BpmnEndErrorEvent(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

