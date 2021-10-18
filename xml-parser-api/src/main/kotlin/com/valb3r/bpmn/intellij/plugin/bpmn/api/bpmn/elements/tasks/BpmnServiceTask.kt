package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

data class BpmnServiceTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val expression: String? = null,
        val delegateExpression: String? = null,
        val clazz: String? = null,
        val resultVariableName: String? = null,
        val skipExpression: String? = null,
        val triggerable: Boolean? = null,
        val isForCompensation: Boolean? = null,
        val useLocalScopeForResultVariable: Boolean? = null,
        val incoming: String? = null,
        val outgoing: String? = null,
        // Customizations (Flowable) - http task, camel task,...:
        val type: String?  = null,
        /* BPMN engine specific extensions (intermediate storage) */
        val extensionElements: List<ExtensionElement>? = null,
        /* Flattened extensionElements, for explicitness - these are the target of binding */
        val failedJobRetryTimeCycle: String? = null,
        val fieldsExtension: List<ExtensionField>? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}