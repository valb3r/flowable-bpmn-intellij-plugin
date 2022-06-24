package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateConditionalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
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

    fun isStartEvent(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnStartEvent, is BpmnStartConditionalEvent, is BpmnStartMessageEvent, is BpmnStartErrorEvent,
            is BpmnStartEscalationEvent, is BpmnStartSignalEvent, is BpmnStartTimerEvent
            -> return true
        }
        return false
    }

    fun isStructuralElement(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnSubProcess, is BpmnEventSubprocess, is BpmnCallActivity, is BpmnAdHocSubProcess
            -> return true
        }
        return false
    }

    fun isBoundaryEvents(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnBoundaryCancelEvent, is BpmnBoundaryCompensationEvent, is BpmnBoundaryConditionalEvent, is BpmnBoundaryErrorEvent,
            is BpmnEndEscalationEvent, is BpmnBoundaryMessageEvent, is BpmnBoundarySignalEvent, is BpmnBoundaryTimerEvent
            -> return true
        }
        return false
    }

    fun isIntermediateCatchingEvent(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnIntermediateTimerCatchingEvent, is BpmnIntermediateMessageCatchingEvent, is BpmnIntermediateSignalCatchingEvent,
            is BpmnIntermediateConditionalCatchingEvent
            -> return true
        }
        return false
    }

    fun isIntermediateThrowingEvent(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnIntermediateNoneThrowingEvent, is BpmnIntermediateSignalThrowingEvent, is BpmnIntermediateEscalationThrowingEvent
            -> return true
        }
        return false
    }

    fun isEndEvent(project: Project, bpmnElementId: BpmnElementId) : Boolean {
        when(getElement(project, bpmnElementId)) {
            is BpmnEndEvent, is BpmnEndErrorEvent, is BpmnEndEscalationEvent, is BpmnEndCancelEvent, is BpmnEndTerminateEvent
            -> return true
        }
        return false
    }

    fun getElement(project: Project, bpmnElementId: BpmnElementId) : WithBpmnId? {
        return currentStateProvider(project).currentState().elementByBpmnId[bpmnElementId]?.element
    }

    fun mutateStartEvent(popup: JBPopupMenu, focus: BpmnElementId)
}