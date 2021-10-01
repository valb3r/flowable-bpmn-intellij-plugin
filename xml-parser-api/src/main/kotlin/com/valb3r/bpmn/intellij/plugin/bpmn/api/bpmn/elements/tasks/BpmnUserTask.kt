package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

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
    val skipExpression: String?,
    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    val fieldsExtension: List<ExtensionField>? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}