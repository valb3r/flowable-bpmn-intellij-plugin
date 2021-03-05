package com.valb3r.bpmn.intellij.plugin.core.actions.copypaste

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import java.awt.geom.Point2D


fun copyToClipboard() {
    val state = lastRenderedState() ?: return
    if (!state.canCopyOrCut()) {
        return
    }
    copyPasteActionHandler().copy(state.state, state.elementsById)
}

fun cutToClipboard() {
    val state = lastRenderedState() ?: return
    if (!state.canCopyOrCut()) {
        return
    }
    copyPasteActionHandler().cut(state.state, updateEventsRegistry(), state.elementsById)
    currentCanvas().clearSelection()
    currentCanvas().repaint()
}

fun pasteFromClipboard(sceneLocation: Point2D.Float, parent: BpmnElementId) {
    val data = copyPasteActionHandler().paste(sceneLocation, parent) ?: return
    // TODO - cursor position update
    updateEventsRegistry().addEvents( data.shapes.toMutableList() + data.edges.toMutableList())
    currentCanvas().clearSelection()
    currentCanvas().selectElements(data.selectElements.toSet())
    currentCanvas().repaint()
}