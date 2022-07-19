package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike

data class BpmnMailTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val documentation: String? = null,
        val async: Boolean? = null,
        val isForCompensation: Boolean? = null,
        val headers: String? = null,
        val to: String? = null,
        val from: String? = null,
        val subject: String? = null,
        val cc: String? = null,
        val bcc: String? = null,
        val text: String? = null,
        val html: String? = null,
        val charset: String? = null,
        val incoming: List<String>? = null,
        val outgoing: List<String>? = null,
        val executionListener: List<ExeсutionListener>? = null
): WithBpmnId, BpmnTaskAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}