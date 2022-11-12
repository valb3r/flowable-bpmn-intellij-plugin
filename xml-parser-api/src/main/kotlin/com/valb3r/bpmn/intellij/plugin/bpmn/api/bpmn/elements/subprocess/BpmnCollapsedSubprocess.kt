package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStructuralElementAlike

data class BpmnCollapsedSubprocess(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val triggeredByEvent: Boolean? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
        val transactionalSubprocess: Boolean // can't set default for @KotlinBuilder
): WithBpmnId, BpmnStructuralElementAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}