package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

open class BpmnExtensionElement

data class ExtensionElement(val name: String? = null, val string: String? = null, val expression: String?): BpmnExtensionElement()

data class ExtensionField(val name: String? = null, val string: String? = null, val expression: String?)

data class ExtensionEventPayload(val source: String? = null, val target: String? = null, val type: String?)

data class ExeсutionListener(val clazz: String?, val fields: List<ListenerField>?)

data class ListenerField(val name: String?, val string: String?)

data class ExtensionFormProperty(
    val id: String?, val name: String?, val type: String?, val expression: String?, val variable: String?, val default: String?,
    val datePattern: String?, val value: List<ExtensionFormPropertyValue>?
)

data class ExtensionFormPropertyValue(val id: String?, val name: String?)

data class UnmappedProperty(val name: String? = null, val string: String? = null)

data class ExtensionFromEvent(
    val eventType: String? = null, val triggerEventType: String? = null, val eventName: String? = null, val channelKey: String? = null,
    val channelName: String?  = null, val channelDestination: String? = null, val triggerEventName: String? = null,
    val triggerChannelKey: String? = null, val triggerChannelName: String? = null, val triggerChannelDestination: String? = null,
    val keyDetectionType: String? = null, val keyDetectionValue: String? = null, val channelType: String? = null, val triggerChannelType: String? = null
)

