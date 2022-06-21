package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.listDefaultPrint
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

    if (event.property.indexCascades) {
        state[event.bpmnElementId]?.view()?.filter { it.key.indexInGroupArrayName == event.property.indexInGroupArrayName }?.forEach { (k, _) ->
            uiEventCascade(event, cascades, k)
        }
    }

    event.property.explicitIndexCascades?.forEach {
        val type = PropertyType.valueOf(it)
        state.forEach { (id, props) ->
            props.getAll(type).filter { it.value == event.referencedValue }.forEach { prop ->
                uiEventCascade(event.copy(bpmnElementId = id, propertyIndex = prop.index), cascades, type)
            }
        }
    }

    addEventByDependProps(event, state, cascades)

    defaultPrintIfNeed(event, state, cascades)

    updateEventsRegistry(project).addEvents(listOf(event) + cascades)
}

private fun defaultPrintIfNeed(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    if (listDefaultPrint.filter { it.headProp == event.property }.isNotEmpty()) {
        listDefaultPrint.filter { it.headProp == event.property }.forEach {
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
}

private fun addEventByDependProps(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    if (event.property.isHeadType && event.newValue.isEmpty()) {
        state[event.bpmnElementId]!!.filter { k, _ -> k.dependecies.contains(event.property) }.forEach { prop ->
            cascades += StringValueUpdatedEvent(
                event.bpmnElementId,
                prop.first,
                "",
                propertyIndex = prop.second.index
            )
        }
    }
}

private fun uiEventCascade(
    event: StringValueUpdatedEvent,
    cascades: MutableList<Event>,
    cascadePropTo: PropertyType
) {
    val index = event.propertyIndex ?: listOf()
    if (event.newValue.isBlank()) {
        cascades += UiOnlyValueRemovedEvent(event.bpmnElementId, cascadePropTo, index)
    }
    cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, cascadePropTo, index, index.dropLast(1) + event.newValue)
}