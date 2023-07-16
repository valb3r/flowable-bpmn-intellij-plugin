package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.CascadeGroup
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.defaultXmlNestedValues
import com.valb3r.bpmn.intellij.plugin.core.events.IndexUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.UiOnlyValueRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry

internal fun emitStringUpdateWithCascadeIfNeeded(state: Map<BpmnElementId, PropertyTable>, event: StringValueUpdatedEvent, project: Project) {
    val cascades = mutableListOf<Event>()
    handleValueCascadeGloballyBasedOnReferenceWithoutIndex(event, state, cascades)
    handleValueCascadeWithinSameElementWithoutIndex(event, state, cascades)
    handleIndexChanges(event, state, cascades)
    addStaticDependentFieldsToXml(event, state, cascades)
    val orderedEvents = (listOf(event) + cascades).sortedBy {
        when (it) {
            is PropertyUpdateWithId -> it.property.inCascadeOrder
            else -> Int.MAX_VALUE
        }
    }
    updateEventsRegistry(project).addEvents(orderedEvents)
}

private fun handleValueCascadeWithinSameElementWithoutIndex(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    val element = state[event.bpmnElementId]!!
    element
        .filter { k, _ -> k.updatedByWithinSameElement == event.property }
        .forEach { prop ->
        cascades += StringValueUpdatedEvent(
            event.bpmnElementId,
            prop.first,
            if (null != prop.first.onUpdatedByUseHardcodedValue) prop.first.onUpdatedByUseHardcodedValue as String else event.newValue,
            null,
            null,
            propertyIndex = prop.second.index
        )
    }
}

private fun handleValueCascadeGloballyBasedOnReferenceWithoutIndex(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    if (null == event.referencedValue) {
        return
    }

    state.forEach { (id, props) ->
        props.filter { k, _ -> k.updatedBy == event.property }.filter { it.second.value == event.referencedValue }
            .forEach { prop ->
                cascades += StringValueUpdatedEvent(
                    id,
                    prop.first,
                    event.newValue,
                    event.referencedValue,
                    null,
                    propertyIndex = prop.second.index
                )
            }
    }
}

private fun handleIndexChanges(
    event: StringValueUpdatedEvent,
    state: Map<BpmnElementId, PropertyTable>,
    cascades: MutableList<Event>
) {
    if (event.property.indexCascades == CascadeGroup.PARENTS_CASCADE || event.property.indexCascades == CascadeGroup.FLAT) {
        state[event.bpmnElementId]?.view()?.filter { it.key.group?.contains(event.property.group?.last()) == true }
            ?.forEach { (cascadeType, cascadeProperty) ->
                cascadeProperty.filter { cascaded ->
                    event.propertyIndex?.forEachIndexed { index, s ->
                        if (null != cascaded.index && cascaded.index!![index] != s) {
                            return@filter false
                        }
                    }
                    return@filter true
                }.forEach {
                    handleGroupIndexChangeDueToCascading(event, cascades, cascadeType, it.index)
                }
            }
    }

    event.property.explicitIndexCascades?.forEach {
        val type = PropertyType.valueOf(it)
        state.forEach { (id, props) ->
            props.getAll(type).filter { it.value == event.referencedValue }.forEach { prop ->
                handleGroupIndexChangeDueToCascading(
                    event.copy(bpmnElementId = id, propertyIndex = prop.index),
                    cascades,
                    type
                )
            }
        }
    }
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

private fun handleGroupIndexChangeDueToCascading(
    event: StringValueUpdatedEvent,
    cascades: MutableList<Event>,
    cascadePropTo: PropertyType,
    cascadePropertyIndex: List<String>? = null
) {
    val index = cascadePropertyIndex ?: event.propertyIndex ?: listOf()
    if (event.newValue.isBlank()) {
        cascades += UiOnlyValueRemovedEvent(event.bpmnElementId, cascadePropTo, index)
        if (event.property.indexCascades == CascadeGroup.FLAT) {
            cascades += StringValueUpdatedEvent(event.bpmnElementId, cascadePropTo, "")
        }
        return
    }
    cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, cascadePropTo, index, calculateIndex(event, cascadePropertyIndex))
}

fun calculateIndex(event: StringValueUpdatedEvent, cascadeIndex: List<String>?): List<String> {
    if (cascadeIndex == null) return (event.propertyIndex?.dropLast(1) ?: listOf()) + event.newValue
    val newIndexPropertyList = cascadeIndex.toMutableList()
    val replacedElementIndex = event.propertyIndex!!.size - 1
    newIndexPropertyList[replacedElementIndex] = event.newValue
    return newIndexPropertyList.toList()
}
