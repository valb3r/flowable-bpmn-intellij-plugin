package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

/**
 * Is created from BpmnServiceTask based on its type
 */
data class BpmnDecisionTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val exclusive: Boolean?,
        val isForCompensation: Boolean?,
        val decisionTableReferenceKey: String? = null,
        val decisionTaskThrowErrorOnNoHits: Boolean? = null,
        val fallbackToDefaultTenantCdata: Boolean? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}