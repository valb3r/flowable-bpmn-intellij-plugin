package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike

/**
 * Is created from BpmnServiceTask based on its type
 */
data class BpmnShellTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val isForCompensation: Boolean? = null,
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
        val directory: String? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
): WithBpmnId, BpmnTaskAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}