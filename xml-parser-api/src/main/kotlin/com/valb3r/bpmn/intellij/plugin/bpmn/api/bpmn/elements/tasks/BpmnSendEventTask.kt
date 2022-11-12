package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike

data class BpmnSendEventTask(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val async: Boolean? = null,
    val exclusive: Boolean? = null,
    val triggerable: Boolean? = null,

    /* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
    var eventExtensionElements: List<ExtensionFromEvent>? = null,
    val extensionElementsMappingPayloadToEvent: List<ExtensionEventPayload>? = null,
    val extensionElementsMappingPayloadFromEvent: List<ExtensionEventPayload>? = null,
    val executionListener: List<ExeсutionListener>? = null,
    ): WithBpmnId, BpmnTaskAlike {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}