package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

@KotlinBuilder
data class BpmnScriptTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val isForCompensation: Boolean?,
        val scriptBody: String?,
        val scriptFormat: String?,
        val autoStoreVariables: Boolean?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}