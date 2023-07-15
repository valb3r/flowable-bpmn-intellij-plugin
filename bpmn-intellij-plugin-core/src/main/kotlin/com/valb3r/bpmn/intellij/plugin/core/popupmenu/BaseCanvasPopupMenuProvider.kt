package com.valb3r.bpmn.intellij.plugin.core.popupmenu

import ShapeCreator
import ShapeTypeChange
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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.*
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.cutToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.pasteFromClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.saveDiagramToPng
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.CanvasPopupMenuProvider
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JMenu
import javax.swing.JPopupMenu

// Functional
private val COPY_ICON = IconLoader.getIcon("/icons/actions/copy.png", BpmnAdHocSubProcess::class.java)
private val CUT_ICON = IconLoader.getIcon("/icons/actions/cut.png", BaseCanvasPopupMenuProvider::class.java)
private val PASTE_ICON = IconLoader.getIcon("/icons/actions/paste.png", BaseCanvasPopupMenuProvider::class.java)
private val SAVE_TO_PNG_ICON = IconLoader.getIcon("/icons/actions/save-to-png.png", BaseCanvasPopupMenuProvider::class.java)

// Events
// Start
private val START_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_MESSAGE_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/message-start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_ERROR_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/error-start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_SIGNAL_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/signal-start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_TIMER_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/timer-start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_CONDITIONAL_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/conditional-start-event.png", BaseCanvasPopupMenuProvider::class.java)
private val START_ESCALATION_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/escalation-start-event.png", BaseCanvasPopupMenuProvider::class.java)

// End
private val END_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/end-event.png", BaseCanvasPopupMenuProvider::class.java)
private val CANCEL_END_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/cancel-end-event.png", BaseCanvasPopupMenuProvider::class.java)
private val ERROR_END_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/error-end-event.png", BaseCanvasPopupMenuProvider::class.java)
private val TERMINATE_END_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/terminate-end-event.png", BaseCanvasPopupMenuProvider::class.java)
private val ESCALATION_END_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/escalation-end-event.png", BaseCanvasPopupMenuProvider::class.java)

// Boundary
private val BOUNDARY_CANCEL_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/cancel-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_COMPENSATION_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/compensation-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_ERROR_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/error-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_MESSAGE_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/message-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_SIGNAL_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/signal-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_TIMER_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/timer-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_CONDITIONAL_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/conditional-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)
private val BOUNDARY_ESCALATION_EVENT_ICON = IconLoader.getIcon("/icons/popupmenu/escalation-boundary-event.png", BaseCanvasPopupMenuProvider::class.java)

// Intermediate events
// Catch
private val INTERMEDIATE_TIMER_CATCHING_ICON = IconLoader.getIcon("/icons/popupmenu/timer-catch-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_MESSAGE_CATCHING_ICON = IconLoader.getIcon("/icons/popupmenu/message-catch-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_SIGNAL_CATCHING_ICON = IconLoader.getIcon("/icons/popupmenu/signal-catch-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_CONDITIONAL_CATCHING_ICON = IconLoader.getIcon("/icons/popupmenu/conditional-catch-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_LINK_CATCHING_ICON = IconLoader.getIcon("/icons/popupmenu/intermediate-link-catch-event.png", BaseCanvasPopupMenuProvider::class.java)

// Throw
private val INTERMEDIATE_NONE_THROWING_ICON = IconLoader.getIcon("/icons/popupmenu/none-throw-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_SIGNAL_THROWING_ICON = IconLoader.getIcon("/icons/popupmenu/signal-throw-event.png", BaseCanvasPopupMenuProvider::class.java)
private val INTERMEDIATE_ESCALATION_THROWING_ICON = IconLoader.getIcon("/icons/popupmenu/escalation-throw-event.png", BaseCanvasPopupMenuProvider::class.java)

// Service-task alike
private val TASK_ICON = IconLoader.getIcon("/icons/popupmenu/call-activity.png", BaseCanvasPopupMenuProvider::class.java)
private val SERVICE_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/service-task.png", BaseCanvasPopupMenuProvider::class.java)
private val USER_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/user-task.png", BaseCanvasPopupMenuProvider::class.java)
private val SCRIPT_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/script-task.png", BaseCanvasPopupMenuProvider::class.java)
private val BUSINESS_RULE_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/business-rule-task.png", BaseCanvasPopupMenuProvider::class.java)
private val RECEIVE_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/receive-task.png", BaseCanvasPopupMenuProvider::class.java)
private val MANUAL_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/manual-task.png", BaseCanvasPopupMenuProvider::class.java)
private val CAMEL_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/camel-task.png", BaseCanvasPopupMenuProvider::class.java)
private val MAIL_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/mail-task.png", BaseCanvasPopupMenuProvider::class.java)
private val MULE_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/mule-task.png", BaseCanvasPopupMenuProvider::class.java)
private val DECISION_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/decision-task.png", BaseCanvasPopupMenuProvider::class.java)
private val HTTP_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/http-task.png", BaseCanvasPopupMenuProvider::class.java)
private val SHELL_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/shell-task.png", BaseCanvasPopupMenuProvider::class.java)
private val EXTERNAL_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/external-task.png", BaseCanvasPopupMenuProvider::class.java)
private val SEND_TASK_ICON = IconLoader.getIcon("/icons/popupmenu/send-task.png", BaseCanvasPopupMenuProvider::class.java)

// Sub process alike
private val CALL_ACTIVITY_ICON = IconLoader.getIcon("/icons/popupmenu/call-activity.png", BaseCanvasPopupMenuProvider::class.java)
private val SUB_PROCESS_ICON = IconLoader.getIcon("/icons/popupmenu/subprocess.png", BaseCanvasPopupMenuProvider::class.java)
private val EVENT_SUB_PROCESS_ICON = IconLoader.getIcon("/icons/popupmenu/event-subprocess.png", BaseCanvasPopupMenuProvider::class.java)
private val ADHOC_SUB_PROCESS_ICON = IconLoader.getIcon("/icons/popupmenu/adhoc-subprocess.png", BaseCanvasPopupMenuProvider::class.java)

// Gateway
private val EXCLUSIVE_GATEWAY_ICON = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png", BaseCanvasPopupMenuProvider::class.java)
private val PARALLEL_GATEWAY_ICON = IconLoader.getIcon("/icons/popupmenu/parallel-gateway.png", BaseCanvasPopupMenuProvider::class.java)
private val INCLUSIVE_GATEWAY_ICON = IconLoader.getIcon("/icons/popupmenu/inclusive-gateway.png", BaseCanvasPopupMenuProvider::class.java)
private val EVENT_GATEWAY_ICON = IconLoader.getIcon("/icons/popupmenu/event-gateway.png", BaseCanvasPopupMenuProvider::class.java)
private val COMPLEX_GATEWAY_ICON = IconLoader.getIcon("/icons/popupmenu/complex-gateway.png", BaseCanvasPopupMenuProvider::class.java)

data class MenuItemDef(
    val name: String,
    val menuIcon: Icon,
    val newElementListener: (p: Project, sceneLocation: Point2D.Float, focus: BpmnElementId) -> ActionListener,
    val elementMutationListener: (p: Project, focus: BpmnElementId) -> ActionListener
)

val currentPopupMenuUiComponentSupplier = AtomicReference { id: String -> JBPopupMenu() } // Not map as is global
val currentPopupMenuItemUiComponentSupplier = AtomicReference { name: String, icon: Icon -> JBMenuItem(name, icon) } // Not map as is global

const val MAIN_POPUP_MENU = "Main popup menu"
const val SHAPE_CHANGE_POPUP_MENU = "Shape change popup menu"

abstract class BaseCanvasPopupMenuProvider(private val project: Project) : CanvasPopupMenuProvider {

    protected val START_EVENT = MenuItemDef("Start event",  START_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartEvent::class, focus)})
    protected val START_CONDITIONAL_EVENT = MenuItemDef("Start conditional event",  START_CONDITIONAL_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartConditionalEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartConditionalEvent::class, focus)})
    protected val START_MESSAGE_EVENT = MenuItemDef("Start message event",  START_MESSAGE_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartMessageEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartMessageEvent::class, focus)})
    protected val START_ERROR_EVENT = MenuItemDef("Start error event",  START_ERROR_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartErrorEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartErrorEvent::class, focus)})
    protected val START_ESCALATION_EVENT = MenuItemDef("Start escalation event",  START_ESCALATION_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartEscalationEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartEscalationEvent::class, focus)})
    protected val START_SIGNAL_EVENT = MenuItemDef("Start signal event",  START_SIGNAL_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartSignalEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartSignalEvent::class, focus)})
    protected val START_TIMER_EVENT = MenuItemDef("Start timer event",  START_TIMER_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnStartTimerEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnStartTimerEvent::class, focus)})
    protected val BOUNDARY_CANCEL_EVENT = MenuItemDef("Boundary cancel event",  BOUNDARY_CANCEL_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryCancelEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryCancelEvent::class, focus)})
    protected val BOUNDARY_COMPENSATION_EVENT = MenuItemDef("Boundary compensation event",  BOUNDARY_COMPENSATION_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryCompensationEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryCompensationEvent::class, focus)})
    protected val BOUNDARY_CONDITIONAL_EVENT = MenuItemDef("Boundary conditional event",  BOUNDARY_CONDITIONAL_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryConditionalEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryConditionalEvent::class, focus)})
    protected val BOUNDARY_ERROR_EVENT = MenuItemDef("Boundary error event",  BOUNDARY_ERROR_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryErrorEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryErrorEvent::class, focus)})
    protected val BOUNDARY_ESCALATION_EVENT = MenuItemDef("Boundary escalation event",  BOUNDARY_ESCALATION_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryEscalationEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryEscalationEvent::class, focus)})
    protected val BOUNDARY_MESSAGE_EVENT = MenuItemDef("Boundary message event",  BOUNDARY_MESSAGE_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryMessageEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryMessageEvent::class, focus)})
    protected val BOUNDARY_SIGNAL_EVENT = MenuItemDef("Boundary signal event",  BOUNDARY_SIGNAL_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundarySignalEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundarySignalEvent::class, focus)})
    protected val BOUNDARY_TIMER_EVENT = MenuItemDef("Boundary timer event",  BOUNDARY_TIMER_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBoundaryTimerEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBoundaryTimerEvent::class, focus)})
    protected val END_EVENT = MenuItemDef("End event",  END_EVENT_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnEndEvent::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnEndEvent::class, focus)})
    protected val END_ERROR_EVENT = MenuItemDef("End error event",  ERROR_END_EVENT_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnEndErrorEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnEndErrorEvent::class, focus)})
    protected val END_ESCALATION_EVENT = MenuItemDef("End escalation event",  ESCALATION_END_EVENT_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnEndEscalationEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnEndEscalationEvent::class, focus)})
    protected val END_CANCEL_EVENT = MenuItemDef("End cancel event",  CANCEL_END_EVENT_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnEndCancelEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnEndCancelEvent::class, focus)})
    protected val END_TERMINATE_EVENT = MenuItemDef("End terminate event",  TERMINATE_END_EVENT_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnEndTerminateEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnEndTerminateEvent::class, focus)})
    protected val INTERMEDIATE_NONE_THROWING_EVENT = MenuItemDef("Intermediate none throwing event",  INTERMEDIATE_NONE_THROWING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateNoneThrowingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateNoneThrowingEvent::class, focus)})
    protected val INTERMEDIATE_SIGNAL_THROWING_EVENT = MenuItemDef("Intermediate signal throwing event",  INTERMEDIATE_SIGNAL_THROWING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateSignalThrowingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateSignalThrowingEvent::class, focus)})
    protected val INTERMEDIATE_ESCALATION_THROWING_EVENT = MenuItemDef("Intermediate escalation throwing event",  INTERMEDIATE_ESCALATION_THROWING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateEscalationThrowingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateEscalationThrowingEvent::class, focus)})
    protected val INTERMEDIATE_TIMER_CATCHING_EVENT = MenuItemDef("Intermediate timer catching event",  INTERMEDIATE_TIMER_CATCHING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateTimerCatchingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateTimerCatchingEvent::class, focus)})
    protected val INTERMEDIATE_MESSAGE_CATCHING_EVENT = MenuItemDef("Intermediate message catching event",  INTERMEDIATE_MESSAGE_CATCHING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateMessageCatchingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateMessageCatchingEvent::class, focus)})
    protected val INTERMEDIATE_SIGNAL_CATCHING_EVENT = MenuItemDef("Intermediate signal catching event",  INTERMEDIATE_SIGNAL_CATCHING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateSignalCatchingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateSignalCatchingEvent::class, focus)})
    protected val INTERMEDIATE_CONDITIONAL_CATCHING_EVENT = MenuItemDef("Intermediate conditional catching event",  INTERMEDIATE_CONDITIONAL_CATCHING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateConditionalCatchingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateConditionalCatchingEvent::class, focus)})
    protected val INTERMEDIATE_LINK_CATCHING_EVENT = MenuItemDef("Intermediate link catching event",  INTERMEDIATE_LINK_CATCHING_ICON, { project, sceneLocation, focus -> ShapeCreator(project, BpmnIntermediateLinkCatchingEvent::class, sceneLocation, focus)}, { project, focus ->  ShapeTypeChange(project, BpmnIntermediateLinkCatchingEvent::class, focus)})
    protected val SUB_PROCESS = MenuItemDef("Sub process",  SUB_PROCESS_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnSubProcess::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnSubProcess::class, focus)})
    protected val EVENT_SUB_PROCESS = MenuItemDef("Event sub process",  EVENT_SUB_PROCESS_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnEventSubprocess::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnEventSubprocess::class, focus)})
    protected val CALL_ACTIVITY = MenuItemDef("Call activity",  CALL_ACTIVITY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnCallActivity::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnCallActivity::class, focus)})
    protected val ADHOC_SUB_PROCESS = MenuItemDef("Adhoc sub process",  ADHOC_SUB_PROCESS_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnAdHocSubProcess::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnAdHocSubProcess::class, focus)})
    protected val EXCLUSIVE_GATEWAY = MenuItemDef("Exclusive gateway",  EXCLUSIVE_GATEWAY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnExclusiveGateway::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnExclusiveGateway::class, focus)})
    protected val PARALLEL_GATEWAY = MenuItemDef("Parallel gateway",  PARALLEL_GATEWAY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnParallelGateway::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnParallelGateway::class, focus)})
    protected val INCLUSIVE_GATEWAY = MenuItemDef("Inclusive gateway",  INCLUSIVE_GATEWAY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnInclusiveGateway::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnInclusiveGateway::class, focus)})
    protected val EVENT_GATEWAY = MenuItemDef("Event gateway",  EVENT_GATEWAY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnEventGateway::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnEventGateway::class, focus)})
    protected val COMPLEX_GATEWAY = MenuItemDef("Complex gateway",  COMPLEX_GATEWAY_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnComplexGateway::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnComplexGateway::class, focus)})
    protected val TASK = MenuItemDef("Task",  TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnTask::class, focus)})
    protected val SEND_TASK = MenuItemDef("Send task",  SEND_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnSendTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnSendTask::class, focus)})
    protected val USER_TASK = MenuItemDef("User Task",  USER_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnUserTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnUserTask::class, focus)})
    protected val SERVICE_TASK = MenuItemDef("Service Task",  SERVICE_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnServiceTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnServiceTask::class, focus)})
    protected val SCRIPT_TASK = MenuItemDef("Script Task",  SCRIPT_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnScriptTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnScriptTask::class, focus)})
    protected val BUSINESS_RULE_TASK = MenuItemDef("Business rule Task",  BUSINESS_RULE_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnBusinessRuleTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnBusinessRuleTask::class, focus)})
    protected val RECEIVE_TASK = MenuItemDef("Receive Task",  RECEIVE_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnReceiveTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnReceiveTask::class, focus)})
    protected val MANUAL_TASK = MenuItemDef("Manual Task",  MANUAL_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnManualTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnManualTask::class, focus)})
    protected val CAMEL_TASK = MenuItemDef("Camel Task",  CAMEL_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnCamelTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnCamelTask::class, focus)})
    protected val HTTP_TASK = MenuItemDef("Http task",  HTTP_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnHttpTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnHttpTask::class, focus)})
    protected val MAIL_TASK = MenuItemDef("Mail task",  MAIL_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnMailTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnMailTask::class, focus)})
    protected val MULE_TASK = MenuItemDef("Mule task",  MULE_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnMuleTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnMuleTask::class, focus)})
    protected val DECISION_TASK = MenuItemDef("Decision task",  DECISION_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnDecisionTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnDecisionTask::class, focus)})
    protected val SHELL_TASK = MenuItemDef("Shell task",  SHELL_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnShellTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnShellTask::class, focus)})
    protected val EXTERNAL_TASK = MenuItemDef("External Worker task", EXTERNAL_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnExternalTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnExternalTask::class, focus)})
    protected val SEND_EVENT_TASK = MenuItemDef("Send event task", SEND_TASK_ICON, {project, sceneLocation, focus -> ShapeCreator(project, BpmnSendEventTask::class, sceneLocation, focus)}, {project, focus ->  ShapeTypeChange(project, BpmnSendEventTask::class, focus)})

    protected fun addItem(menu: JPopupMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    protected fun addCopyAndPasteIfNeeded(popup: JBPopupMenu, sceneLocation: Point2D.Float, parent: BpmnElementId) {
        val renderedState = lastRenderedState(project)
        if (true == renderedState?.canCopyOrCut()) {
            addItem(popup, "Copy", COPY_ICON) { copyToClipboard(project) }
            addItem(popup, "Cut", CUT_ICON) { cutToClipboard(project) }
        }

        if (copyPasteActionHandler(project).hasDataToPaste()) {
            addItem(popup, "Paste", PASTE_ICON) { pasteFromClipboard(project, sceneLocation, parent) }
        }
    }

    override fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu {
        val popup = currentPopupMenuUiComponentSupplier.get()(MAIN_POPUP_MENU)
        addCopyAndPasteIfNeeded(popup, sceneLocation, parent)
        popup.add(startEvents(sceneLocation, parent))
        popup.add(activities(sceneLocation, parent))
        popup.add(structural(sceneLocation, parent))
        popup.add(gateways(sceneLocation, parent))
        popup.add(boundaryEvents(sceneLocation, parent))
        popup.add(intermediateCatchingEvents(sceneLocation, parent))
        popup.add(intermediateThrowingEvents(sceneLocation, parent))
        popup.add(endEvents(sceneLocation, parent))
        addItem(popup, "Save to PNG", SAVE_TO_PNG_ICON) { saveDiagramToPng(project) }
        return popup
    }

    override fun popupChangeShapeType(focus: BpmnElementId): JBPopupMenu {
        val popup = currentPopupMenuUiComponentSupplier.get()(SHAPE_CHANGE_POPUP_MENU)
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

    protected fun JMenu.addNewElementItem(sceneLocation: Point2D.Float, focus: BpmnElementId, menuItem: MenuItemDef) {
        val item = currentPopupMenuItemUiComponentSupplier.get()(menuItem.name, menuItem.menuIcon)
        item.addActionListener(menuItem.newElementListener(project, sceneLocation, focus))
        this.add(item)
    }

    protected fun JPopupMenu.addMutateElementItem(focus: BpmnElementId, menuItem: MenuItemDef) {
        val item = currentPopupMenuItemUiComponentSupplier.get()(menuItem.name, menuItem.menuIcon)
        item.addActionListener(menuItem.elementMutationListener(project, focus))
        this.add(item)
    }
}