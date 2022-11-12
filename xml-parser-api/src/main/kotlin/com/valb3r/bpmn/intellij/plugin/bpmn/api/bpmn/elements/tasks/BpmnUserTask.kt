package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike

data class BpmnUserTask(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val async: Boolean? = null,
    val asyncBefore: Boolean? = null,
    val asyncAfter: Boolean? = null,
    val isForCompensation: Boolean? = null,
    val assignee: String? = null,
    val candidateUsers: String? = null,
    val candidateGroups: String? = null,
    val dueDate: String? = null,
    val category: String? = null,
    val formKey: String? = null,
    val formFieldValidation: Boolean? = null,
    val priority: String? = null,
    val skipExpression: String? = null,
    val incoming: List<String>? = null,
    val outgoing: List<String>? = null,
    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    val formPropertiesExtension: List<ExtensionFormProperty>? = null,
    val executionListener: List<ExeсutionListener>? = null
    ): WithBpmnId, BpmnTaskAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}