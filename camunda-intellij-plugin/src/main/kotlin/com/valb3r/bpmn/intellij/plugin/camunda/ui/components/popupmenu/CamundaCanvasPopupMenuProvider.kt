package com.valb3r.bpmn.intellij.plugin.camunda.ui.components.popupmenu

import ShapeChange
import ShapeCreator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.cutToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.pasteFromClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.saveDiagramToPng
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.CanvasPopupMenuProvider
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import javax.swing.Icon
import javax.swing.JMenu
import javax.swing.JPopupMenu

class CamundaCanvasPopupMenuProvider(private val project: Project) : CanvasPopupMenuProvider {

    // Functional
    private val COPY = IconLoader.getIcon("/icons/actions/copy.png")
    private val CUT = IconLoader.getIcon("/icons/actions/cut.png")
    private val PASTE = IconLoader.getIcon("/icons/actions/paste.png")
    private val SAVE_TO_PNG = IconLoader.getIcon("/icons/actions/save-to-png.png")

    // Events
    // Start
    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
    private val START_CONDITIONAL_EVENT = IconLoader.getIcon("/icons/popupmenu/conditional-start-event.png")
    private val START_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-start-event.png")
    // private val START_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-start-event.png")
    // private val START_ESCALATION_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-start-event.png")
    private val START_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-start-event.png")
    private val START_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-start-event.png")
    // End
    private val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png")
    // private val CANCEL_END_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-end-event.png")
    private val ERROR_END_EVENT = IconLoader.getIcon("/icons/popupmenu/error-end-event.png")
    private val ESCALATION_END_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-end-event.png")
    private val TERMINATE_END_EVENT = IconLoader.getIcon("/icons/popupmenu/terminate-end-event.png")
    // Boundary
//    private val BOUNDARY_CANCEL_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-boundary-event.png")
    private val BOUNDARY_COMPENSATION_EVENT = IconLoader.getIcon("/icons/popupmenu/compensation-boundary-event.png")
    private val BOUNDARY_CONDITIONAL_EVENT = IconLoader.getIcon("/icons/popupmenu/conditional-boundary-event.png")
    private val BOUNDARY_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-boundary-event.png")
    private val BOUNDARY_ESCALATION_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-boundary-event.png")
    private val BOUNDARY_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-boundary-event.png")
    private val BOUNDARY_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-boundary-event.png")
    private val BOUNDARY_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-boundary-event.png")
    // Intermediate events
    // Catch
    private val INTERMEDIATE_TIMER_CATCHING = IconLoader.getIcon("/icons/popupmenu/timer-catch-event.png")
    private val INTERMEDIATE_MESSAGE_CATCHING = IconLoader.getIcon("/icons/popupmenu/message-catch-event.png")
    private val INTERMEDIATE_SIGNAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/signal-catch-event.png")
    private val INTERMEDIATE_CONDITIONAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/conditional-catch-event.png")
    private val INTERMEDIATE_LINK_CATCHING = IconLoader.getIcon("/icons/popupmenu/intermediate-link-catch-event.png")
    // Throw
    private val INTERMEDIATE_NONE_THROWING = IconLoader.getIcon("/icons/popupmenu/none-throw-event.png")
    private val INTERMEDIATE_SIGNAL_THROWING = IconLoader.getIcon("/icons/popupmenu/signal-throw-event.png")
    private val INTERMEDIATE_ESCALATION_THROWING = IconLoader.getIcon("/icons/popupmenu/escalation-throw-event.png")

    // Service-task alike
    private val TASK = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    private val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png")
    private val USER_TASK = IconLoader.getIcon("/icons/popupmenu/user-task.png")
    private val SCRIPT_TASK = IconLoader.getIcon("/icons/popupmenu/script-task.png")
    private val BUSINESS_RULE_TASK = IconLoader.getIcon("/icons/popupmenu/business-rule-task.png")
    private val SEND_TASK = IconLoader.getIcon("/icons/popupmenu/send-task.png")
    private val RECEIVE_TASK = IconLoader.getIcon("/icons/popupmenu/receive-task.png")
    private val MANUAL_TASK = IconLoader.getIcon("/icons/popupmenu/manual-task.png")
    private val EXTERNAL_TASK = IconLoader.getIcon("/icons/popupmenu/external-task.png")
//    private val CAMEL_TASK = IconLoader.getIcon("/icons/popupmenu/camel-task.png")
//    private val HTTP_TASK = IconLoader.getIcon("/icons/popupmenu/http-task.png")
//    private val MAIL_TASK = IconLoader.getIcon("/icons/popupmenu/mail-task.png")
//    private val MULE_TASK = IconLoader.getIcon("/icons/popupmenu/mule-task.png")
//    private val DECISION_TASK = IconLoader.getIcon("/icons/popupmenu/decision-task.png")
//    private val SHELL_TASK = IconLoader.getIcon("/icons/popupmenu/shell-task.png")

    // Sub process alike
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    private val SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/subprocess.png")
    private val EVENT_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/event-subprocess.png")
    private val ADHOC_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/adhoc-subprocess.png")

    // Gateway
    private val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png")
    private val PARALLEL_GATEWAY = IconLoader.getIcon("/icons/popupmenu/parallel-gateway.png")
    private val INCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/inclusive-gateway.png")
    private val EVENT_GATEWAY = IconLoader.getIcon("/icons/popupmenu/event-gateway.png")
    private val COMPLEX_GATEWAY = IconLoader.getIcon("/icons/popupmenu/complex-gateway.png")

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
        addItem(popup, "Save to PNG", SAVE_TO_PNG, ActionListener { saveDiagramToPng(project) })
        return popup
    }

    override fun popupChangeShape(focus: BpmnElementId): JBPopupMenu
    {
        val popup = JBPopupMenu()
        if(isStartEvent(project, focus)) {
            addItem(popup, "Start event", START_EVENT, ShapeChange(project, BpmnStartEvent::class, focus))
            addItem(popup, "Start conditional event", START_CONDITIONAL_EVENT, ShapeChange(project, BpmnStartConditionalEvent::class, focus))
            addItem(popup, "Start message event", START_MESSAGE_EVENT, ShapeChange(project, BpmnStartConditionalEvent::class, focus))
            addItem(popup, "Start signal event", START_SIGNAL_EVENT, ShapeChange(project, BpmnStartSignalEvent::class, focus))
            addItem(popup, "Start timer event", START_TIMER_EVENT, ShapeChange(project, BpmnStartTimerEvent::class, focus))
        } else if(isTask(project, focus)) {
            addItem(popup, "Task", TASK, ShapeChange(project, BpmnTask::class, focus))
            addItem(popup, "User task", USER_TASK, ShapeChange(project, BpmnUserTask::class, focus))
            addItem(popup, "Service task", SERVICE_TASK, ShapeChange(project, BpmnServiceTask::class, focus))
            addItem(popup, "Script task", SCRIPT_TASK, ShapeChange(project, BpmnScriptTask::class, focus))
            addItem(popup, "Business rule task", BUSINESS_RULE_TASK, ShapeChange(project, BpmnBusinessRuleTask::class, focus))
            addItem(popup, "Send task", SEND_TASK, ShapeChange(project, BpmnSendTask::class, focus))
            addItem(popup, "Receive task", RECEIVE_TASK, ShapeChange(project, BpmnReceiveTask::class, focus))
            addItem(popup, "Manual task", MANUAL_TASK, ShapeChange(project, BpmnManualTask::class, focus))
            addItem(popup, "External task", MANUAL_TASK, ShapeChange(project, BpmnExternalTask::class, focus))
        } else if(isStructuralElement(project, focus)) {
            addItem(popup, "Sub process", SUB_PROCESS, ShapeChange(project, BpmnSubProcess::class, focus))
            addItem(popup, "Event sub process", EVENT_SUB_PROCESS, ShapeChange(project, BpmnEventSubprocess::class, focus))
            addItem(popup, "Call activity", CALL_ACTIVITY, ShapeChange(project, BpmnCallActivity::class, focus))
            addItem(popup, "Adhoc sub process", ADHOC_SUB_PROCESS, ShapeChange(project, BpmnAdHocSubProcess::class, focus))
        } else if(isGateway(project, focus)) {
            addItem(popup, "Exclusive gateway", EXCLUSIVE_GATEWAY, ShapeChange(project, BpmnExclusiveGateway::class, focus))
            addItem(popup, "Parallel gateway", PARALLEL_GATEWAY, ShapeChange(project, BpmnParallelGateway::class, focus))
            addItem(popup, "Inclusive gateway", INCLUSIVE_GATEWAY, ShapeChange(project, BpmnInclusiveGateway::class, focus))
            addItem(popup, "Event gateway", EVENT_GATEWAY, ShapeChange(project, BpmnEventGateway::class, focus))
            addItem(popup, "Complex gateway", COMPLEX_GATEWAY, ShapeChange(project, BpmnComplexGateway::class, focus))
        } else if(isBoundaryEvents(project, focus)) {
            addItem(popup, "Boundary compensation event", BOUNDARY_COMPENSATION_EVENT, ShapeChange(project, BpmnBoundaryCompensationEvent::class, focus))
            addItem(popup, "Boundary conditional event", BOUNDARY_CONDITIONAL_EVENT, ShapeChange(project, BpmnStartConditionalEvent::class, focus))
            addItem(popup, "Boundary error event", BOUNDARY_ERROR_EVENT, ShapeChange(project, BpmnBoundaryErrorEvent::class, focus))
            addItem(popup, "Boundary escalation event", BOUNDARY_ESCALATION_EVENT, ShapeChange(project, BpmnBoundaryEscalationEvent::class, focus))
            addItem(popup, "Boundary message event", BOUNDARY_MESSAGE_EVENT, ShapeChange(project, BpmnBoundaryMessageEvent::class, focus))
            addItem(popup, "Boundary signal event", BOUNDARY_SIGNAL_EVENT, ShapeChange(project, BpmnBoundarySignalEvent::class, focus))
            addItem(popup, "Boundary timer event", BOUNDARY_TIMER_EVENT, ShapeChange(project, BpmnBoundaryTimerEvent::class, focus))
        } else if(isIntermediateCatchingEvent(project, focus)) {
            addItem(popup, "Intermediate timer catching event", INTERMEDIATE_TIMER_CATCHING, ShapeChange(project, BpmnIntermediateTimerCatchingEvent::class, focus))
            addItem(popup, "Intermediate message catching event", INTERMEDIATE_MESSAGE_CATCHING, ShapeChange(project, BpmnIntermediateMessageCatchingEvent::class, focus))
            addItem(popup, "Intermediate signal catching event", INTERMEDIATE_SIGNAL_CATCHING, ShapeChange(project, BpmnIntermediateSignalCatchingEvent::class, focus))
            addItem(popup, "Intermediate conditional catching event", INTERMEDIATE_CONDITIONAL_CATCHING, ShapeChange(project, BpmnIntermediateConditionalCatchingEvent::class, focus))
            addItem(popup, "Intermediate link catching event", INTERMEDIATE_LINK_CATCHING, ShapeChange(project, BpmnIntermediateConditionalCatchingEvent::class, focus))
        } else if(isIntermediateThrowingEvent(project, focus)) {
            addItem(popup, "Intermediate none throwing event", INTERMEDIATE_NONE_THROWING, ShapeChange(project, BpmnIntermediateNoneThrowingEvent::class, focus))
            addItem(popup, "Intermediate signal throwing event", INTERMEDIATE_SIGNAL_THROWING, ShapeChange(project, BpmnIntermediateSignalThrowingEvent::class, focus))
            addItem(popup, "Intermediate escalation throwing event", INTERMEDIATE_ESCALATION_THROWING, ShapeChange(project, BpmnIntermediateEscalationThrowingEvent::class, focus))
        } else if(isEndEvent(project, focus)) {
            addItem(popup, "End event", END_EVENT, ShapeChange(project, BpmnEndEvent::class, focus))
            addItem(popup, "End error event", ERROR_END_EVENT, ShapeChange(project, BpmnEndErrorEvent::class, focus))
            addItem(popup, "End escalation event", ESCALATION_END_EVENT, ShapeChange(project, BpmnEndEscalationEvent::class, focus))
            addItem(popup, "End terminate event", TERMINATE_END_EVENT, ShapeChange(project, BpmnEndTerminateEvent::class, focus))
        }
        return popup
    }

    private fun addCopyAndPasteIfNeeded(popup: JBPopupMenu, sceneLocation: Point2D.Float, parent: BpmnElementId) {
        val renderedState = lastRenderedState(project)
        if (true == renderedState?.canCopyOrCut()) {
            addItem(popup, "Copy", COPY, ActionListener { copyToClipboard(project) })
            addItem(popup, "Cut", CUT, ActionListener { cutToClipboard(project) })
        }

        if (copyPasteActionHandler(project).hasDataToPaste()) {
            addItem(popup, "Paste", PASTE, ActionListener { pasteFromClipboard(project, sceneLocation, parent) })
        }
    }

    private fun startEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Start events")
        addItem(menu, "Start event", START_EVENT, ShapeCreator(project, BpmnStartEvent::class, sceneLocation, parent))
        addItem(menu, "Start conditional event", START_CONDITIONAL_EVENT, ShapeCreator(project, BpmnStartConditionalEvent::class, sceneLocation, parent))
        addItem(menu, "Start message event", START_MESSAGE_EVENT, ShapeCreator(project, BpmnStartMessageEvent::class, sceneLocation, parent))
        // Unsupported? addItem(menu, "Start error event", START_ERROR_EVENT, ShapeCreator(project, BpmnStartErrorEvent::class, sceneLocation, parent))
        // Unsupported? addItem(menu, "Start escalation event", START_ESCALATION_EVENT, ShapeCreator(project, BpmnStartEscalationEvent::class, sceneLocation, parent))
        addItem(menu, "Start signal event", START_SIGNAL_EVENT, ShapeCreator(project, BpmnStartSignalEvent::class, sceneLocation, parent))
        addItem(menu, "Start timer event", START_TIMER_EVENT, ShapeCreator(project, BpmnStartTimerEvent::class, sceneLocation, parent))
        return menu
    }

    private fun activities(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Activities")
        addItem(menu, "Task", TASK, ShapeCreator(project, BpmnTask::class, sceneLocation, parent))
        addItem(menu, "User task", USER_TASK, ShapeCreator(project, BpmnUserTask::class, sceneLocation, parent))
        addItem(menu, "Service task", SERVICE_TASK, ShapeCreator(project, BpmnServiceTask::class, sceneLocation, parent))
        addItem(menu, "Script task", SCRIPT_TASK, ShapeCreator(project, BpmnScriptTask::class, sceneLocation, parent))
        addItem(menu, "Business rule task", BUSINESS_RULE_TASK, ShapeCreator(project, BpmnBusinessRuleTask::class, sceneLocation, parent))
        addItem(menu, "Send task", SEND_TASK, ShapeCreator(project, BpmnSendTask::class, sceneLocation, parent))
        addItem(menu, "Receive task", RECEIVE_TASK, ShapeCreator(project, BpmnReceiveTask::class, sceneLocation, parent))
        addItem(menu, "Manual task", MANUAL_TASK, ShapeCreator(project, BpmnManualTask::class, sceneLocation, parent))
        addItem(menu, "External task", EXTERNAL_TASK, ShapeCreator(project, BpmnExternalTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Camel task", CAMEL_TASK, ShapeCreator(project, BpmnCamelTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Http task", HTTP_TASK, ShapeCreator(project, BpmnHttpTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Mail task", MAIL_TASK, ShapeCreator(project, BpmnMailTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Mule task", MULE_TASK, ShapeCreator(project, BpmnMuleTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Decision task", DECISION_TASK, ShapeCreator(project, BpmnDecisionTask::class, sceneLocation, parent))
        // Unsupported addItem(menu, "Shell task", SHELL_TASK, ShapeCreator(project, BpmnShellTask::class, sceneLocation, parent))
        return menu
    }

    private fun structural(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Structural")
        addItem(menu, "Sub process", SUB_PROCESS, ShapeCreator(project, BpmnSubProcess::class, sceneLocation, parent))
        addItem(menu, "Event sub process", EVENT_SUB_PROCESS, ShapeCreator(project, BpmnEventSubprocess::class, sceneLocation, parent))
        addItem(menu, "Call activity", CALL_ACTIVITY, ShapeCreator(project, BpmnCallActivity::class, sceneLocation, parent))
        addItem(menu, "Adhoc sub process", ADHOC_SUB_PROCESS, ShapeCreator(project, BpmnAdHocSubProcess::class, sceneLocation, parent))
        return menu
    }

    private fun gateways(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Gateways")
        addItem(menu, "Exclusive gateway", EXCLUSIVE_GATEWAY, ShapeCreator(project, BpmnExclusiveGateway::class, sceneLocation, parent))
        addItem(menu, "Parallel gateway", PARALLEL_GATEWAY, ShapeCreator(project, BpmnParallelGateway::class, sceneLocation, parent))
        addItem(menu, "Inclusive gateway", INCLUSIVE_GATEWAY, ShapeCreator(project, BpmnInclusiveGateway::class, sceneLocation, parent))
        addItem(menu, "Event gateway", EVENT_GATEWAY, ShapeCreator(project, BpmnEventGateway::class, sceneLocation, parent))
        addItem(menu, "Complex gateway", COMPLEX_GATEWAY, ShapeCreator(project, BpmnComplexGateway::class, sceneLocation, parent))
        return menu
    }

    private fun boundaryEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Boundary events")
        // addItem(menu, "Boundary cancel event", BOUNDARY_CANCEL_EVENT, ShapeCreator(project, BpmnBoundaryCancelEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary compensation event", BOUNDARY_COMPENSATION_EVENT, ShapeCreator(project, BpmnBoundaryCompensationEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary conditional event", BOUNDARY_CONDITIONAL_EVENT, ShapeCreator(project, BpmnBoundaryConditionalEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary error event", BOUNDARY_ERROR_EVENT, ShapeCreator(project, BpmnBoundaryErrorEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary escalation event", BOUNDARY_ESCALATION_EVENT, ShapeCreator(project, BpmnBoundaryEscalationEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary message event", BOUNDARY_MESSAGE_EVENT, ShapeCreator(project, BpmnBoundaryMessageEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary signal event", BOUNDARY_SIGNAL_EVENT, ShapeCreator(project, BpmnBoundarySignalEvent::class, sceneLocation, parent))
        addItem(menu, "Boundary timer event", BOUNDARY_TIMER_EVENT, ShapeCreator(project, BpmnBoundaryTimerEvent::class, sceneLocation, parent))
        return menu
    }

    private fun intermediateCatchingEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate catching events")
        addItem(menu, "Intermediate timer catching event", INTERMEDIATE_TIMER_CATCHING, ShapeCreator(project, BpmnIntermediateTimerCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate message catching event", INTERMEDIATE_MESSAGE_CATCHING, ShapeCreator(project, BpmnIntermediateMessageCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate signal catching event", INTERMEDIATE_SIGNAL_CATCHING, ShapeCreator(project, BpmnIntermediateSignalCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate conditional catching event", INTERMEDIATE_CONDITIONAL_CATCHING, ShapeCreator(project, BpmnIntermediateConditionalCatchingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate link catching event", INTERMEDIATE_LINK_CATCHING, ShapeCreator(project, BpmnIntermediateLinkCatchingEvent::class, sceneLocation, parent))
        return menu
    }

    private fun intermediateThrowingEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate throwing events")
        addItem(menu, "Intermediate none throwing event", INTERMEDIATE_NONE_THROWING, ShapeCreator(project, BpmnIntermediateNoneThrowingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate signal throwing event", INTERMEDIATE_SIGNAL_THROWING, ShapeCreator(project, BpmnIntermediateSignalThrowingEvent::class, sceneLocation, parent))
        addItem(menu, "Intermediate escalation throwing event", INTERMEDIATE_ESCALATION_THROWING, ShapeCreator(project, BpmnIntermediateEscalationThrowingEvent::class, sceneLocation, parent))
        return menu
    }

    private fun endEvents(sceneLocation: Point2D.Float, parent: BpmnElementId): JMenu {
        val menu = JMenu("End events")
        addItem(menu, "End event", END_EVENT, ShapeCreator(project, BpmnEndEvent::class, sceneLocation, parent))
        addItem(menu, "End error event", ERROR_END_EVENT, ShapeCreator(project, BpmnEndErrorEvent::class, sceneLocation, parent))
        addItem(menu, "End escalation event", ESCALATION_END_EVENT, ShapeCreator(project, BpmnEndEscalationEvent::class, sceneLocation, parent))
        // Unsupported? addItem(menu, "End cancel event", CANCEL_END_EVENT, ShapeCreator(project, BpmnEndCancelEvent::class, sceneLocation, parent))
        addItem(menu, "End terminate event", TERMINATE_END_EVENT, ShapeCreator(project, BpmnEndTerminateEvent::class, sceneLocation, parent))
        return menu
    }

    private fun addItem(menu: JMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    private fun addItem(menu: JPopupMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }
}