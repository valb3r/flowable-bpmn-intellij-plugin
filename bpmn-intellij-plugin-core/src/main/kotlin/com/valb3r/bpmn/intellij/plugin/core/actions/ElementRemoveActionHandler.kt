package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import java.util.*

private val currentRemoveActionHandler = Collections.synchronizedMap(WeakHashMap<Project,  ElementRemoveActionHandler>())

fun currentRemoveActionHandler(project: Project): ElementRemoveActionHandler {
    return currentRemoveActionHandler.computeIfAbsent(project) {
        ElementRemoveActionHandler(project)
    }
}

class ElementRemoveActionHandler(private val project: Project) {

    fun deleteSelectedElements() {
        val state = lastRenderedState(project)?.state ?: return

        val toDelete = mutableListOf<Event>()
        toDelete += state.ctx.selectedIds.mapNotNull { state.elemMap[it] }.flatMap {
            val elemRemoval = it.getEventsToElementWithItsDiagram()
            return@flatMap elemRemoval.diagram + elemRemoval.bpmn + elemRemoval.other
        }
        val inOrder = toDelete.sortedBy {
            when (it) {
                is PropertyUpdateWithId -> return@sortedBy 0
                else -> return@sortedBy 100
            }
        }

        updateEventsRegistry(project).addEvents(inOrder)

        currentCanvas(project).repaint()
    }
}