package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.geom.Point2D
import java.util.*
import javax.swing.JMenu
import javax.swing.JPopupMenu


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

    fun startEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun activities(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun structural(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun gateways(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun boundaryEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun intermediateCatchingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun intermediateThrowingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu
    fun endEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu

    fun mutateStartEvent(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateBoundaryEvents(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateEndEvent(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateIntermediateThrowingEvent(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateIntermediateCatchingEvent(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateStructuralElement(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateGateway(popup: JPopupMenu, focus: BpmnElementId)
    fun mutateTask(popup: JPopupMenu, focus: BpmnElementId)
}
