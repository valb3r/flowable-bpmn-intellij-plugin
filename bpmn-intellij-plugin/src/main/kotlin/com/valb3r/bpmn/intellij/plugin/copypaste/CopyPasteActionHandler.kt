package com.valb3r.bpmn.intellij.plugin.copypaste

import com.fasterxml.jackson.databind.ObjectMapper
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.edges.BaseEdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.shapes.ShapeRenderElement
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
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

private val DATA_FLAVOR = DataFlavor(String::class.java, "Flowable BPMN IntelliJ editor plugin clipboard data")

data class ClipboardAddEvents(val shapes: MutableList<BpmnShapeObjectAddedEvent>, val edges: MutableList<BpmnEdgeObjectAddedEvent>)

class CopyPasteActionHandler {

    private val ROOT_NAME = "__:-:-:ROOT"

    private val mapper = ObjectMapper()

    fun copy(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
        val orderedIds = ensureRootElementsComeFirst(idsToCopy, ctx, elementsById)
        val toCopy = ClipboardAddEvents(mutableListOf(), mutableListOf())
        for (diagramId in orderedIds) {
            elementToAddEvents(ctx, diagramId, elementsById, toCopy, false)
        }

        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(FlowableClipboardFlavor(mapper.writeValueAsString(toCopy)), null)
    }

    fun cut(idsToCut: MutableList<DiagramElementId>, ctx: RenderState, updateEvents: ProcessModelUpdateEvents, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun paste(updateEvents: ProcessModelUpdateEvents, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            val data: FlowableClipboardFlavor = clipboard.getData(DATA_FLAVOR) as FlowableClipboardFlavor
        } catch (ex: Exception) {
            // NOP
        }
    }

    private fun ensureRootElementsComeFirst(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>): MutableList<DiagramElementId> {
        return idsToCopy
                .sortedByDescending { ctx.currentState.elementByDiagramId[it]?.let {id -> elementsById[id] }?.zIndex() ?: 0 }
                .toMutableList()
    }

    private fun elementToAddEvents(
            ctx: RenderState,
            diagramId: DiagramElementId,
            elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>,
            events: ClipboardAddEvents,
            preserveRoot: Boolean) {
        val bpmnId = ctx.currentState.elementByDiagramId[diagramId] ?: return
        val withParentId = ctx.currentState.elementByBpmnId[bpmnId] ?: return
        val props = ctx.currentState.elemPropertiesByStaticElementId[bpmnId] ?: return
        val renderElem = elementsById[bpmnId] ?: return
        fun detachParentIfNeeded() = if (preserveRoot) withParentId else withParentId.copy(parent = BpmnElementId(ROOT_NAME))

        when (renderElem) {
            is ShapeRenderElement -> {
                events.shapes += BpmnShapeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.shapeElem,
                        props
                )
                renderElem.children.forEach {elementToAddEvents(ctx, it.elementId, elementsById, events, true)}
            }
            is BaseEdgeRenderElement -> {
                events.edges += BpmnEdgeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.edgeElem,
                        props
                )
            }
        }
    }

    private class FlowableClipboardFlavor(data: String): StringSelection(data) {

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            return DATA_FLAVOR.equals(flavor)
        }
    }
}