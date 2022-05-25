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
    val eventType: String? = null,
//    val eventName: String? = null,
//    val mappingToEventPayload: String? = null,
//    val channelKey: String? = null,
//    val channelName: String? = null,
//    // TODO channelType must be list of selection
//    val channelDestination: String? = null,
//    val triggerEventType: String? = null,
//    val triggerEventName: String? = null,
//    val triggerChannelKey: String? = null,
//    val triggerChannelName: String? = null,
//    // TODO triggerChannelType must be list of selection
//    val triggerChannelDestination: String? = null,
//    // TODO Mapping from event payload
//    // TODO Correlation parameters
//    val eventKeyFixedValue: String? = null,
//    val eventKeyJsonField: String? = null,
//    val eventKeyJsonPointer: String? = null,

/* BPMN engine specific extensions (intermediate storage) */
    val extensionElements: List<ExtensionElement>? = null,
    /* Flattened extensionElements, for explicitness - these are the target of binding */
//    val formPropertiesExtension: List<ExtensionFormProperty>? = null
    val eventExtensionElements: List<ExtensionFromEvent>? = null

): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}