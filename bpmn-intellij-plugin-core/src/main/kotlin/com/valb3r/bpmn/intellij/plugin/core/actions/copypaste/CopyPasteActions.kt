package com.valb3r.bpmn.intellij.plugin.core.actions.copypaste

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import java.awt.geom.Point2D


fun copyToClipboard(project: Project) {
    val state = lastRenderedState(project) ?: return
    if (!state.canCopyOrCut()) {
        return
    }
    copyPasteActionHandler(project).copy(state.state, state.elementsById)
}

fun cutToClipboard(project: Project) {
    val state = lastRenderedState(project) ?: return
    if (!state.canCopyOrCut()) {
        return
    }
    copyPasteActionHandler(project).cut(state.state, updateEventsRegistry(project), state.elementsById)
    currentCanvas(project).clearSelection()
    currentCanvas(project).repaint()
}

fun pasteFromClipboard(project: Project, sceneLocation: Point2D.Float, parent: BpmnElementId) {
    val data = copyPasteActionHandler(project).paste(sceneLocation, parent) ?: return
    // TODO - cursor position update
    updateEventsRegistry(project).addEvents( data.shapes.toMutableList() + data.edges.toMutableList())
    currentCanvas(project).clearSelection()
    currentCanvas(project).selectElements(data.selectElements.toSet())
    currentCanvas(project).repaint()
}