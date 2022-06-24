package com.valb3r.bpmn.intellij.activiti.plugin.popupmenu

import ShapeChange
import ShapeCreator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.*
import com.valb3r.bpmn.intellij.plugin.core.actions.saveDiagramToPng
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.BaseCanvasPopupMenuProvider
import java.awt.geom.Point2D
import javax.swing.JMenu

class ActivitiCanvasPopupMenuProvider(val project: Project) : BaseCanvasPopupMenuProvider(project) {

    override fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu {
        val popup = JBPopupMenu()

        addCopyAndPasteIfNeeded(popup, sceneLocation, parent)
        popup.add(startEvents(sceneLocation, parent))
        popup.add(activities(sceneLocation, parent))
        popup.add(structural(sceneLocation, parent))
        popup.add(gateways(sceneLocation, parent))
        popup.add(boundaryEvents(sceneLocation, parent))
        popup.add(intermediateCatchingEvents(sceneLocation, parent))
        popup.add(intermediateThrowingEvents(sceneLocation, parent))
        popup.add(endEvents(sceneLocation, parent))
        addItem(popup, "Save to PNG", SAVE_TO_PNG) { saveDiagramToPng(project) }
        return popup
    }

    override fun popupChangeShape(focus: BpmnElementId): JBPopupMenu {
        val popup = JBPopupMenu()
        val focusedElem = getElement(project, focus)
        when (focusedElem) {
            is BpmnStartEventAlike -> mutateStartEvent(popup, focus)
            is BpmnBoundaryEventAlike -> mutateBoundaryEvents(popup, focus)
            is BpmnTaskAlike -> mutateTask(popup, focus)
            is BpmnGatewayAlike -> mutateGateway(popup, focus)
            is BpmnStructuralElementAlike -> mutateStructuralElement(popup, focus)
            is IntermediateThrowingEventAlike -> mutateIntermediateThrowingEvent(popup, focus)
            is IntermediateCatchingEventAlike -> mutateIntermediateCatchingEvent(popup, focus)
            is EndEventAlike -> mutateEndEvent(popup, focus)
        }

        return popup
    }

    override fun mutateStartEvent(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Start event", START_EVENT, ShapeChange(project, BpmnStartEvent::class, focus))
        addItem(popup, "Start timer event", START_TIMER_EVENT, ShapeChange(project, BpmnStartTimerEvent::class, focus))
        addItem(popup, "Start signal event", START_SIGNAL_EVENT, ShapeChange(project, BpmnStartSignalEvent::class, focus))
        addItem(popup, "Start error event", START_ERROR_EVENT, ShapeChange(project, BpmnStartErrorEvent::class, focus))
    }

    override fun mutateBoundaryEvents(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Boundary error event", BOUNDARY_ERROR_EVENT, ShapeChange(project, BpmnBoundaryErrorEvent::class, focus))
        addItem(popup, "Boundary timer event", BOUNDARY_TIMER_EVENT, ShapeChange(project, BpmnBoundaryTimerEvent::class, focus))
        addItem(popup, "Boundary signal event", BOUNDARY_SIGNAL_EVENT, ShapeChange(project, BpmnBoundarySignalEvent::class, focus))
        addItem(popup, "Boundary message event", BOUNDARY_MESSAGE_EVENT, ShapeChange(project, BpmnBoundaryMessageEvent::class, focus))
        addItem(popup, "Boundary cancel event", BOUNDARY_CANCEL_EVENT, ShapeChange(project, BpmnBoundaryCancelEvent::class, focus))
        addItem(popup, "Boundary compensation event", BOUNDARY_COMPENSATION_EVENT, ShapeChange(project, BpmnBoundaryCompensationEvent::class, focus))
    }

    override fun mutateTask(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "User Task", USER_TASK, ShapeChange(project, BpmnUserTask::class, focus))
        addItem(popup, "Service Task", SERVICE_TASK, ShapeChange(project, BpmnServiceTask::class, focus))
        addItem(popup, "Script Task", SCRIPT_TASK, ShapeChange(project, BpmnScriptTask::class, focus))
        addItem(popup, "Business rule Task", BUSINESS_RULE_TASK, ShapeChange(project, BpmnBusinessRuleTask::class, focus))
        addItem(popup, "Receive Task", RECEIVE_TASK, ShapeChange(project, BpmnReceiveTask::class, focus))
        addItem(popup, "Manual Task", MANUAL_TASK, ShapeChange(project, BpmnManualTask::class, focus))
        addItem(popup, "Mail task", MAIL_TASK, ShapeChange(project, BpmnMailTask::class, focus))
        addItem(popup, "Camel Task", CAMEL_TASK, ShapeChange(project, BpmnCamelTask::class, focus))
        addItem(popup, "Mule task", MULE_TASK, ShapeChange(project, BpmnMuleTask::class, focus))
        addItem(popup, "Decision task", DECISION_TASK, ShapeChange(project, BpmnDecisionTask::class, focus))
    }

    override fun mutateGateway(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Exclusive gateway", EXCLUSIVE_GATEWAY, ShapeChange(project, BpmnExclusiveGateway::class, focus))
        addItem(popup, "Parallel gateway", PARALLEL_GATEWAY, ShapeChange(project, BpmnParallelGateway::class, focus))
        addItem(popup, "Inclusive gateway", INCLUSIVE_GATEWAY, ShapeChange(project, BpmnInclusiveGateway::class, focus))
        addItem(popup, "Event gateway", EVENT_GATEWAY, ShapeChange(project, BpmnEventGateway::class, focus))
    }

    override fun mutateStructuralElement(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Sub process", SUB_PROCESS, ShapeChange(project, BpmnSubProcess::class, focus))
        addItem(popup, "Event sub process", EVENT_SUB_PROCESS, ShapeChange(project, BpmnEventSubprocess::class, focus))
        addItem(popup, "Call activity", CALL_ACTIVITY, ShapeChange(project, BpmnCallActivity::class, focus))
    }

    override fun mutateIntermediateThrowingEvent(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Intermediate none throwing event", INTERMEDIATE_NONE_THROWING, ShapeChange(project, BpmnIntermediateNoneThrowingEvent::class, focus))
        addItem(popup, "Intermediate signal throwing event", INTERMEDIATE_SIGNAL_THROWING, ShapeChange(project, BpmnIntermediateSignalThrowingEvent::class, focus))
    }

    override fun mutateIntermediateCatchingEvent(popup: JBPopupMenu, focus: BpmnElementId) {
        addItem(popup, "Intermediate timer catching event", INTERMEDIATE_TIMER_CATCHING, ShapeChange(project, BpmnIntermediateTimerCatchingEvent::class, focus))
        addItem(popup, "Intermediate message catching event", INTERMEDIATE_MESSAGE_CATCHING, ShapeChange(project, BpmnIntermediateMessageCatchingEvent::class, focus))
        addItem(popup, "Intermediate signal catching event", INTERMEDIATE_SIGNAL_CATCHING, ShapeChange(project, BpmnIntermediateSignalCatchingEvent::class, focus))
    }

    override fun mutateEndEvent(popup: JBPopupMenu, focus: BpmnElementId){
        addItem(popup, "End event", END_EVENT, ShapeChange(project, BpmnEndEvent::class, focus))
        addItem(popup, "End error event", ERROR_END_EVENT, ShapeChange(project, BpmnEndErrorEvent::class, focus))
        addItem(popup, "End cancel event", CANCEL_END_EVENT, ShapeChange(project, BpmnEndCancelEvent::class, focus))
        addItem(popup, "End terminate event", TERMINATE_END_EVENT, ShapeChange(project, BpmnEndTerminateEvent::class, focus))
    }

    private fun startEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Start events")
        addItem(menu, "Start event", START_EVENT, ShapeCreator(project, BpmnStartEvent::class, sceneLocation, parent))
        addItem(menu, "Start timer event", START_TIMER_EVENT, ShapeCreator(project, BpmnStartTimerEvent::class, sceneLocation, parent))
        addItem(menu, "Start signal event", START_SIGNAL_EVENT, ShapeCreator(project, BpmnStartSignalEvent::class, sceneLocation, parent))
        addItem(menu, "Start message event", START_MESSAGE_EVENT, ShapeCreator(project, BpmnStartMessageEvent::class, sceneLocation, parent))
        addItem(menu, "Start error event", START_ERROR_EVENT, ShapeCreator(project, BpmnStartErrorEvent::class, sceneLocation, parent))
        return menu
    }

    private fun activities(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Activities")
        addItem(menu, "User task", USER_TASK, ShapeCreator(project, BpmnUserTask::class, sceneLocation, parent))
        addItem(menu, "Service task", SERVICE_TASK, ShapeCreator(project, BpmnServiceTask::class, sceneLocation, parent))
        addItem(menu, "Script task", SCRIPT_TASK, ShapeCreator(project, BpmnScriptTask::class, sceneLocation, parent))
        addItem(menu, "Business rule task", BUSINESS_RULE_TASK, ShapeCreator(project, BpmnBusinessRuleTask::class, sceneLocation, parent))
        addItem(menu, "Receive task", RECEIVE_TASK, ShapeCreator(project, BpmnReceiveTask::class, sceneLocation, parent))
        addItem(menu, "Manual task", MANUAL_TASK, ShapeCreator(project, BpmnManualTask::class, sceneLocation, parent))
        addItem(menu, "Mail task", MAIL_TASK, ShapeCreator(project, BpmnMailTask::class, sceneLocation, parent))
        addItem(menu, "Camel task", CAMEL_TASK, ShapeCreator(project, BpmnCamelTask::class, sceneLocation, parent))
        addItem(menu, "Mule task", MULE_TASK, ShapeCreator(project, BpmnMuleTask::class, sceneLocation, parent))
        addItem(menu, "Decision task", DECISION_TASK, ShapeCreator(project, BpmnDecisionTask::class, sceneLocation, parent))
        return menu
    }

    private fun structural(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Structural")
        addItem(menu, "Sub process", SUB_PROCESS, ShapeCreator(project, BpmnSubProcess::class, sceneLocation, parent))
        addItem(menu, "Event sub process", EVENT_SUB_PROCESS, ShapeCreator(project, BpmnEventSubprocess::class, sceneLocation, parent))
        addItem(menu, "Call activity", CALL_ACTIVITY, ShapeCreator(project, BpmnCallActivity::class, sceneLocation, parent))
        return menu
    }

    private fun gateways(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Gateways")
        addItem(menu, "Exclusive gateway", EXCLUSIVE_GATEWAY, ShapeCreator(project, BpmnExclusiveGateway::class, sceneLocation, parent))
        addItem(menu, "Parallel gateway", PARALLEL_GATEWAY, ShapeCreator(project, BpmnParallelGateway::class, sceneLocation, parent))
        addItem(menu, "Inclusive gateway", INCLUSIVE_GATEWAY, ShapeCreator(project, BpmnInclusiveGateway::class, sceneLocation, parent))
        addItem(menu, "Event gateway", EVENT_GATEWAY, ShapeCreator(project, BpmnEventGateway::class, sceneLocation, parent))
        return menu
    }

    private fun boundaryEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Boundary events")
        addItem(menu, "Boundary error event", BOUNDARY_ERROR_EVENT, ShapeCreator(project, BpmnBoundaryErrorEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary timer event", BOUNDARY_TIMER_EVENT, ShapeCreator(project, BpmnBoundaryTimerEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary signal event", BOUNDARY_SIGNAL_EVENT, ShapeCreator(project, BpmnBoundarySignalEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary message event", BOUNDARY_MESSAGE_EVENT, ShapeCreator(project, BpmnBoundaryMessageEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary cancel event", BOUNDARY_CANCEL_EVENT, ShapeCreator(project, BpmnBoundaryCancelEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary compensation event", BOUNDARY_COMPENSATION_EVENT, ShapeCreator(project, BpmnBoundaryCompensationEvent::class, sceneLocation, parent))
        return menu
    }

    private fun intermediateCatchingEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate catching events")
        addItem(menu, "Intermediate timer catching event", INTERMEDIATE_TIMER_CATCHING, ShapeCreator(project, BpmnIntermediateTimerCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate message catching event", INTERMEDIATE_MESSAGE_CATCHING, ShapeCreator(project, BpmnIntermediateMessageCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate signal catching event", INTERMEDIATE_SIGNAL_CATCHING, ShapeCreator(project, BpmnIntermediateSignalCatchingEvent::class, sceneLocation, parent))
        return menu
    }

    private fun intermediateThrowingEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate throwing events")
        addItem(menu, "Intermediate none throwing event", INTERMEDIATE_NONE_THROWING, ShapeCreator(project, BpmnIntermediateNoneThrowingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate signal throwing event", INTERMEDIATE_SIGNAL_THROWING, ShapeCreator(project, BpmnIntermediateSignalThrowingEvent::class, sceneLocation, parent))
        return menu
    }

    private fun endEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("End events")
        addItem(menu, "End event", END_EVENT, ShapeCreator(project, BpmnEndEvent::class, sceneLocation, parent))
        addItem(menu, "End error event", ERROR_END_EVENT, ShapeCreator(project, BpmnEndErrorEvent::class, sceneLocation, parent))
        addItem(menu, "End cancel event", CANCEL_END_EVENT, ShapeCreator(project, BpmnEndCancelEvent::class, sceneLocation, parent))
        addItem(menu, "End terminate event", TERMINATE_END_EVENT, ShapeCreator(project, BpmnEndTerminateEvent::class, sceneLocation, parent))
        return menu
    }

}