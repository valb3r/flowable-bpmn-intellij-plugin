package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

/**
 * Is created from BpmnServiceTask based on its type
 */
@KotlinBuilder
data class BpmnShellTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val exclusive: Boolean?,
        val isForCompensation: Boolean?,
        val command: String? = null,
        val arg1: String? = null,
        val arg2: String? = null,
        val arg3: String? = null,
        val arg4: String? = null,
        val arg5: String? = null,
        val wait: String? = null,
        val cleanEnv: String? = null,
        val errorCodeVariable: String? = null,
        val outputVariable: String? = null,
        val directory: String? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}