package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike

data class BpmnSendTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val clazz: String? = null,
        val expression: String? = null,
        val delegateExpression: String? = null,
        // Customizations (Camunda)
        val type: String? = null,
        val topic: String? = null,
        /* BPMN engine specific extensions (intermediate storage) */
        val extensionElements: List<ExtensionElement>? = null,
        /* Flattened extensionElements, for explicitness - these are the target of binding */
        val failedJobRetryTimeCycle: String? = null,
        val fieldsExtension: List<ExtensionField>? = null,
        val executionListener: List<ExeсutionListener>? = null
): WithBpmnId, BpmnTaskAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}