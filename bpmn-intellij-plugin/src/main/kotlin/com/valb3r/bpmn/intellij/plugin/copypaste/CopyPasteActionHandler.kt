package com.valb3r.bpmn.intellij.plugin.copypaste

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
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

    fun copy(state: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun cut(state: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun paste(state: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }
}