package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import javax.print.attribute.standard.JobPriority

data class BpmnExternalTask(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val jobTopic: String? = null,
        val async: Boolean? = null,
        val asyncBefore: Boolean? = null,
        val asyncAfter: Boolean? = null,
        val exclusive: Boolean? = null,
        val taskPriority: String? = null,
        val isForCompensation: Boolean? = null,
        // Customizations (Flowable) - http task, camel task,...:
        val type: String?  = null,
        /* BPMN engine specific extensions (intermediate storage) */
        val extensionElements: List<ExtensionElement>? = null,
        /* Flattened extensionElements, for explicitness - these are the target of binding */
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}