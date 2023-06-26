package com.valb3r.bpmn.intellij.activiti.plugin.popupmenu

import ShapeCreator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndCancelEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndErrorEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndTerminateEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
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

class ActivitiCanvasPopupMenuProvider(private val project: Project) : CanvasPopupMenuProvider {

    // Functional
    private val COPY = IconLoader.getIcon("/icons/actions/copy.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val CUT = IconLoader.getIcon("/icons/actions/cut.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val PASTE = IconLoader.getIcon("/icons/actions/paste.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val SAVE_TO_PNG = IconLoader.getIcon("/icons/actions/save-to-png.png", ActivitiCanvasPopupMenuProvider::class.java)

    // Events
    // Start
    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val START_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-start-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val START_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-start-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val START_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-start-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val START_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-start-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    // End
    private val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val CANCEL_END_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-end-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val ERROR_END_EVENT = IconLoader.getIcon("/icons/popupmenu/error-end-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val TERMINATE_END_EVENT = IconLoader.getIcon("/icons/popupmenu/terminate-end-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    // Boundary
    private val BOUNDARY_CANCEL_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BOUNDARY_COMPENSATION_EVENT = IconLoader.getIcon("/icons/popupmenu/compensation-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BOUNDARY_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BOUNDARY_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BOUNDARY_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BOUNDARY_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-boundary-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    // Intermediate events
    // Catch
    private val INTERMEDIATE_TIMER_CATCHING = IconLoader.getIcon("/icons/popupmenu/timer-catch-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val INTERMEDIATE_MESSAGE_CATCHING = IconLoader.getIcon("/icons/popupmenu/message-catch-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val INTERMEDIATE_SIGNAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/signal-catch-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    // Throw
    private val INTERMEDIATE_NONE_THROWING = IconLoader.getIcon("/icons/popupmenu/none-throw-event.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val INTERMEDIATE_SIGNAL_THROWING = IconLoader.getIcon("/icons/popupmenu/signal-throw-event.png", ActivitiCanvasPopupMenuProvider::class.java)

    // Service-task alike
    private val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val USER_TASK = IconLoader.getIcon("/icons/popupmenu/user-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val SCRIPT_TASK = IconLoader.getIcon("/icons/popupmenu/script-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val BUSINESS_RULE_TASK = IconLoader.getIcon("/icons/popupmenu/business-rule-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val RECEIVE_TASK = IconLoader.getIcon("/icons/popupmenu/receive-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val MANUAL_TASK = IconLoader.getIcon("/icons/popupmenu/manual-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val CAMEL_TASK = IconLoader.getIcon("/icons/popupmenu/camel-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val MAIL_TASK = IconLoader.getIcon("/icons/popupmenu/mail-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val MULE_TASK = IconLoader.getIcon("/icons/popupmenu/mule-task.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val DECISION_TASK = IconLoader.getIcon("/icons/popupmenu/decision-task.png", ActivitiCanvasPopupMenuProvider::class.java)

    // Sub process alike
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/subprocess.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val EVENT_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/event-subprocess.png", ActivitiCanvasPopupMenuProvider::class.java)

    // Gateway
    private val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val PARALLEL_GATEWAY = IconLoader.getIcon("/icons/popupmenu/parallel-gateway.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val INCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/inclusive-gateway.png", ActivitiCanvasPopupMenuProvider::class.java)
    private val EVENT_GATEWAY = IconLoader.getIcon("/icons/popupmenu/event-gateway.png", ActivitiCanvasPopupMenuProvider::class.java)

    override fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu {
        val popup = JBPopupMenu()

        addCopyAndpasteIfNeeded(popup, sceneLocation, parent)
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

    private fun addCopyAndpasteIfNeeded(popup: JBPopupMenu, sceneLocation: Point2D.Float, parent: BpmnElementId) {
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