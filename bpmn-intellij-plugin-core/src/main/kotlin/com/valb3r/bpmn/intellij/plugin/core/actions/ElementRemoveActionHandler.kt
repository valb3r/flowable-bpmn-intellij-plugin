package com.valb3r.bpmn.intellij.plugin.core.actions

import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import java.util.concurrent.atomic.AtomicReference

private val currentRemoveActionHandler = AtomicReference<ElementRemoveActionHandler>()

fun currentRemoveActionHandler(): ElementRemoveActionHandler {
    return currentRemoveActionHandler.updateAndGet {
        if (null == it) {
            return@updateAndGet ElementRemoveActionHandler()
        }

        return@updateAndGet it
    }
}

class ElementRemoveActionHandler {

    fun deleteElem() {
        val state = lastRenderedState()?.state ?: return
        val targetIds = state.ctx.selectedIds.filter {
            val area = state.elemMap[it]?.areaType
            area == AreaType.SHAPE_THAT_NESTS || area == AreaType.SHAPE || area == AreaType.EDGE
        }

        updateEventsRegistry().addElementRemovedEvent(
            targetIds.map { DiagramElementRemovedEvent(it) },
            targetIds.mapNotNull { state.currentState.elementByDiagramId[it] }.map { BpmnElementRemovedEvent(it) }
        )

        currentCanvas().repaint()
    }
}