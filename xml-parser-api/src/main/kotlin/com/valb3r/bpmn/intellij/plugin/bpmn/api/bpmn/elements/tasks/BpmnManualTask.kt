package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId

/**
 * Is created from BpmnServiceTask based on its type
 */
@KotlinBuilder
data class BpmnManualTask(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val async: Boolean?,
        val exclusive: Boolean?,
        val isForCompensation: Boolean?
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}