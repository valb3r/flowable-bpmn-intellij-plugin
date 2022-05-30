package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
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

    fun popupChangeShape(focus: BpmnElementId): JBPopupMenu

    fun isTask(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnTask, is BpmnUserTask, is BpmnScriptTask, is BpmnServiceTask, is BpmnBusinessRuleTask,
            is BpmnSendTask, is BpmnReceiveTask, is BpmnCamelTask, is BpmnHttpTask, is BpmnMuleTask, is BpmnDecisionTask, is BpmnShellTask, is BpmnMailTask,
            is BpmnManualTask
            -> return true
        }
        return false
    }

    fun isGateway(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnExclusiveGateway, is BpmnParallelGateway, is BpmnInclusiveGateway, is BpmnEventGateway, is BpmnComplexGateway
            -> return true
        }
        return false
    }

    fun getElement(project: Project, bpmnElementId: BpmnElementId) : WithBpmnId? {
        return currentStateProvider(project).currentState().elementByBpmnId[bpmnElementId]?.element
    }
}