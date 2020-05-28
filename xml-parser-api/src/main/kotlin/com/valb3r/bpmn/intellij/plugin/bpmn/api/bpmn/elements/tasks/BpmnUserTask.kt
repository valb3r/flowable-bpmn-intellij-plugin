package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

@KotlinBuilder
data class BpmnUserTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val isForCompensation: Boolean?,
        val assignee: String?,
        val dueDate: String?,
        val category: String?,
        val formKey: String?,
        val formFieldValidation: Boolean?,
        val priority: String?,
        val skipExpression: String?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}