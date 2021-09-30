package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnServiceTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val exclusive: Boolean?,
        val expression: String?,
        val delegateExpression: String?,
        val clazz: String?,
        val resultVariableName: String?,
        val skipExpression: String?,
        val triggerable: Boolean?,
        val isForCompensation: Boolean?,
        val useLocalScopeForResultVariable: Boolean?,
        // Customizations (Flowable) - http task, camel task,...:
        val type: String?  = null,
        val extensionElements: List<ExtensionElement>? = null,
        val failedJobRetryTimeCycleExtension: String? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}