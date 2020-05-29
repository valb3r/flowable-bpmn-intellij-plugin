package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

@KotlinBuilder
data class BpmnAdHocSubProcess(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val completionCondition: CompletionCondition?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

@KotlinBuilder
data class CompletionCondition(
        val condition: String?
)