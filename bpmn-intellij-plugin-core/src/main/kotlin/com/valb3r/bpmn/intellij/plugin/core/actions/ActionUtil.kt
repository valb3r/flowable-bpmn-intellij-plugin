package com.valb3r.bpmn.intellij.plugin.core.actions

import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState

fun removeElements(state: RenderState, diagramElementIds: List<Any>): List<Event> {
    val toDelete = mutableListOf<Event>()
    toDelete += diagramElementIds.mapNotNull { state.elemMap[it] }.flatMap {
        val elemRemoval = it.getEventsToElementWithItsDiagram()
        return@flatMap elemRemoval.diagram + elemRemoval.bpmn + elemRemoval.other
    }
    val inOrder = toDelete.sortedBy {
        when (it) {
            is PropertyUpdateWithId -> return@sortedBy 0
            else -> return@sortedBy 100
        }
    }
    return inOrder
}