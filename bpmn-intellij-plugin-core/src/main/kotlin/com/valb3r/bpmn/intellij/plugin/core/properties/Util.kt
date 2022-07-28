package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.CascadeGroup
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.defaultXmlNestedValues
import com.valb3r.bpmn.intellij.plugin.core.events.IndexUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.UiOnlyValueRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry

internal fun emitStringUpdateWithCascadeIfNeeded(state: Map<BpmnElementId, PropertyTable>, event: StringValueUpdatedEvent, project: Project) {
    val cascades = mutableListOf<Event>()
    if (null != event.referencedValue) {
        state.forEach { (id, props) ->
            props.filter { k, _ -> k.updatedBy == event.property }.filter { it.second.value == event.referencedValue }.forEach { prop ->
                cascades += StringValueUpdatedEvent(id, prop.first, event.newValue, event.referencedValue, null, propertyIndex = prop.second.index)
            }
        }
    }
    if (event.property.indexCascades == CascadeGroup.PARENTS_CASCADE || event.property.indexCascades == CascadeGroup.FLAT) {
        state[event.bpmnElementId]?.view()?.filter { it.key.group?.contains(event.property.group?.last()) == true }
            ?.forEach { (k, _) ->
                eventCascade(event, cascades, k)
            }
    }

    event.property.explicitIndexCascades?.forEach {
        val type = PropertyType.valueOf(it)
        state.forEach { (id, props) ->
            props.getAll(type).filter { it.value == event.referencedValue }.forEach { prop ->
                eventCascade(event.copy(bpmnElementId = id, propertyIndex = prop.index), cascades, type)
            }
        }
    }

    addStaticDependentFieldsToXml(event, state, cascades)
    updateEventsRegistry(project).addEvents(listOf(event) + cascades)
}

/**
 * Adds static dependent fields to XML if parent element is created
 */
private fun addStaticDependentFieldsToXml(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    defaultXmlNestedValues.filter { it.headProp == event.property }.forEach {
        state[event.bpmnElementId]!!.filter { k, _ -> k == it.dependProp }.forEach { prop ->
            cascades += StringValueUpdatedEvent(
                event.bpmnElementId,
                prop.first,
                it.valueDependProp,
                propertyIndex = prop.second.index
            )
        }
    }
}

private fun eventCascade(
    event: StringValueUpdatedEvent,
    cascades: MutableList<Event>,
    cascadePropTo: PropertyType
) {
    val index = event.propertyIndex ?: listOf()
    if (event.newValue.isBlank()) {
        cascades += UiOnlyValueRemovedEvent(event.bpmnElementId, cascadePropTo, index)
        if(event.property.indexCascades == CascadeGroup.FLAT){
            cascades += StringValueUpdatedEvent(event.bpmnElementId, cascadePropTo, "")
        }
    }
    cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, cascadePropTo, index, index.dropLast(1) + event.newValue)
}