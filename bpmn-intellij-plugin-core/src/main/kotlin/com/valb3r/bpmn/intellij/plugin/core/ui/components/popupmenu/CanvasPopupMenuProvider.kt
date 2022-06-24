package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.geom.Point2D
import java.util.*


private val popupMenuProvider = Collections.synchronizedMap(WeakHashMap<Project,  CanvasPopupMenuProvider>())

fun popupMenuProvider(project: Project): CanvasPopupMenuProvider {
    return popupMenuProvider[project]!!
}

fun registerPopupMenuProvider(project: Project, provider: CanvasPopupMenuProvider) {
    popupMenuProvider[project] = provider
}

interface CanvasPopupMenuProvider {
    fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu

    fun popupChangeShapeType(focus: BpmnElementId): JBPopupMenu

    fun getElement(project: Project, bpmnElementId: BpmnElementId) : WithBpmnId? {
        return currentStateProvider(project).currentState().elementByBpmnId[bpmnElementId]?.element
    }

    fun mutateStartEvent(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateBoundaryEvents(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateEndEvent(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateIntermediateThrowingEvent(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateIntermediateCatchingEvent(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateStructuralElement(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateGateway(popup: JBPopupMenu, focus: BpmnElementId)
    fun mutateTask(popup: JBPopupMenu, focus: BpmnElementId)
}
