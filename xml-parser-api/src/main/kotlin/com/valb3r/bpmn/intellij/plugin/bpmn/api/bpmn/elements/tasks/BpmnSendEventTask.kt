package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*

data class BpmnSendEventTask(
    override val id: BpmnElementId,
    val name: String? = null,
    val documentation: String? = null,
    val async: Boolean? = null,
    val exclusive: Boolean? = null,
    val triggerable: Boolean? = null,
    // TODO Execution listener
//    // TODO channelType must be list of selection
//    // TODO triggerChannelType must be list of selection

//    // TODO Mapping from event payload
//    // TODO Correlation parameters


/* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
//    val formPropertiesExtension: List<ExtensionFormProperty>? =1 null
//    val fieldsExtension: List<ExtensionField>? = null,
    var eventExtensionElements: List<ExtensionFromEvent>? = null,
    val extensionElementsMappingPayloadToEvent: List<ExtensionEventPayload>? = null,
    val extensionElementsMappingPayloadFromEvent: List<ExtensionEventPayload>? = null,
    ): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}