package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent

fun computeCascadeChangeOfBpmnIncomingOutgoingIndex(ownerElementBpmnId: BpmnElementId?, props: Map<PropertyType, Map<BpmnElementId, Property>>, propertyType: PropertyType): List<StringValueUpdatedEvent> {
    val defaultIndex = emptyList<StringValueUpdatedEvent>()
    val elementId = ownerElementBpmnId ?: return defaultIndex
    return props[propertyType]
        ?.filter { it.value.value == elementId.id }
        ?.map { StringValueUpdatedEvent(it.key, propertyType, "", propertyIndex = it.value.index) } ?: emptyList()
}