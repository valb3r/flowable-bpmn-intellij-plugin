package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

/**
 * Is created from BpmnServiceTask based on its type
 */
data class BpmnHttpTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val isForCompensation: Boolean? = null,
        val requestMethod: String? = null,
        val requestUrl: String? = null,
        val requestHeaders: String? = null,
        val requestBody: String? = null,
        val requestBodyEncoding: String? = null,
        val requestTimeout: String? = null,
        val disallowRedirects: Boolean? = null,
        val failStatusCodes: String? = null,
        val handleStatusCodes: String? = null,
        val responseVariableName: String? = null,
        val ignoreException: String? = null,
        val saveRequestVariables: String? = null,
        val saveResponseParameters: String? = null,
        val resultVariablePrefix: String? = null,
        val saveResponseParametersTransient: String? = null,
        val saveResponseVariableAsJson: String? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}