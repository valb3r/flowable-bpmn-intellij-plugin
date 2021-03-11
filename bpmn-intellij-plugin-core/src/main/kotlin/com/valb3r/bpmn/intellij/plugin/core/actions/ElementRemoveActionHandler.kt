package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
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

    fun deleteElem() {
        val state = lastRenderedState(project)?.state ?: return
        val targetIds = state.ctx.selectedIds.filter {
            val area = state.elemMap[it]?.areaType
            area == AreaType.SHAPE_THAT_NESTS || area == AreaType.SHAPE || area == AreaType.EDGE
        }

        updateEventsRegistry(project).addElementRemovedEvent(
            targetIds.map { DiagramElementRemovedEvent(it) },
            targetIds.mapNotNull { state.currentState.elementByDiagramId[it] }.map { BpmnElementRemovedEvent(it) }
        )

        currentCanvas(project).repaint()
    }
}