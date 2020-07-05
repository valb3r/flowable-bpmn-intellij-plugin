package com.valb3r.bpmn.intellij.plugin.copypaste

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import java.util.concurrent.atomic.AtomicReference

private val copyPasteActionHandler = AtomicReference<CopyPasteActionHandler>()

fun copyPasteActionHandler(): CopyPasteActionHandler {
    return copyPasteActionHandler.updateAndGet {
        if (null == it) {
            return@updateAndGet CopyPasteActionHandler()
        }

        return@updateAndGet it
    }
}

class CopyPasteActionHandler {

    fun copy(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun cut(idsToCut: MutableList<DiagramElementId>, ctx: RenderState, updateEvents: ProcessModelUpdateEvents, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun paste(updateEvents: ProcessModelUpdateEvents, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }
}