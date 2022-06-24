package com.valb3r.bpmn.intellij.plugin.core.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyPasteActionHandler
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.cutToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.pasteFromClipboard
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.CanvasPopupMenuProvider
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import javax.swing.Icon
import javax.swing.JMenu
import javax.swing.JPopupMenu

abstract class BaseCanvasPopupMenuProvider(private val project: Project) : CanvasPopupMenuProvider {
    // Functional
    protected val COPY = IconLoader.getIcon("/icons/actions/copy.png")
    protected val CUT = IconLoader.getIcon("/icons/actions/cut.png")
    protected val PASTE = IconLoader.getIcon("/icons/actions/paste.png")
    protected val SAVE_TO_PNG = IconLoader.getIcon("/icons/actions/save-to-png.png")

    // Events
    // Start
    protected val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
    protected val START_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-start-event.png")
    protected val START_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-start-event.png")
    protected val START_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-start-event.png")
    protected val START_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-start-event.png")
    protected val START_CONDITIONAL_EVENT = IconLoader.getIcon("/icons/popupmenu/conditional-start-event.png")
    protected val START_ESCALATION_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-start-event.png")

    // End
    protected val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png")
    protected val CANCEL_END_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-end-event.png")
    protected val ERROR_END_EVENT = IconLoader.getIcon("/icons/popupmenu/error-end-event.png")
    protected val TERMINATE_END_EVENT = IconLoader.getIcon("/icons/popupmenu/terminate-end-event.png")
    protected val ESCALATION_END_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-end-event.png")

    // Boundary
    protected val BOUNDARY_CANCEL_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-boundary-event.png")
    protected val BOUNDARY_COMPENSATION_EVENT = IconLoader.getIcon("/icons/popupmenu/compensation-boundary-event.png")
    protected val BOUNDARY_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-boundary-event.png")
    protected val BOUNDARY_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-boundary-event.png")
    protected val BOUNDARY_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-boundary-event.png")
    protected val BOUNDARY_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-boundary-event.png")
    protected val BOUNDARY_CONDITIONAL_EVENT = IconLoader.getIcon("/icons/popupmenu/conditional-boundary-event.png")
    protected val BOUNDARY_ESCALATION_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-boundary-event.png")

    // Intermediate events
    // Catch
    protected val INTERMEDIATE_TIMER_CATCHING = IconLoader.getIcon("/icons/popupmenu/timer-catch-event.png")
    protected val INTERMEDIATE_MESSAGE_CATCHING = IconLoader.getIcon("/icons/popupmenu/message-catch-event.png")
    protected val INTERMEDIATE_SIGNAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/signal-catch-event.png")
    protected val INTERMEDIATE_CONDITIONAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/conditional-catch-event.png")
    protected val INTERMEDIATE_LINK_CATCHING = IconLoader.getIcon("/icons/popupmenu/intermediate-link-catch-event.png")

    // Throw
    protected val INTERMEDIATE_NONE_THROWING = IconLoader.getIcon("/icons/popupmenu/none-throw-event.png")
    protected val INTERMEDIATE_SIGNAL_THROWING = IconLoader.getIcon("/icons/popupmenu/signal-throw-event.png")
    protected val INTERMEDIATE_ESCALATION_THROWING = IconLoader.getIcon("/icons/popupmenu/escalation-throw-event.png")

    // Service-task alike
    protected val TASK = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    protected val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png")
    protected val USER_TASK = IconLoader.getIcon("/icons/popupmenu/user-task.png")
    protected val SCRIPT_TASK = IconLoader.getIcon("/icons/popupmenu/script-task.png")
    protected val BUSINESS_RULE_TASK = IconLoader.getIcon("/icons/popupmenu/business-rule-task.png")
    protected val RECEIVE_TASK = IconLoader.getIcon("/icons/popupmenu/receive-task.png")
    protected val MANUAL_TASK = IconLoader.getIcon("/icons/popupmenu/manual-task.png")
    protected val CAMEL_TASK = IconLoader.getIcon("/icons/popupmenu/camel-task.png")
    protected val MAIL_TASK = IconLoader.getIcon("/icons/popupmenu/mail-task.png")
    protected val MULE_TASK = IconLoader.getIcon("/icons/popupmenu/mule-task.png")
    protected val DECISION_TASK = IconLoader.getIcon("/icons/popupmenu/decision-task.png")
    protected val HTTP_TASK = IconLoader.getIcon("/icons/popupmenu/http-task.png")
    protected val SHELL_TASK = IconLoader.getIcon("/icons/popupmenu/shell-task.png")
    protected val EXTERNAL_TASK = IconLoader.getIcon("/icons/popupmenu/external-task.png")
    protected val SEND_TASK = IconLoader.getIcon("/icons/popupmenu/send-task.png")

    // Sub process alike
    protected val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    protected val SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/subprocess.png")
    protected val EVENT_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/event-subprocess.png")
    protected val ADHOC_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/adhoc-subprocess.png")

    // Gateway
    protected val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png")
    protected val PARALLEL_GATEWAY = IconLoader.getIcon("/icons/popupmenu/parallel-gateway.png")
    protected val INCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/inclusive-gateway.png")
    protected val EVENT_GATEWAY = IconLoader.getIcon("/icons/popupmenu/event-gateway.png")
    protected val COMPLEX_GATEWAY = IconLoader.getIcon("/icons/popupmenu/complex-gateway.png")

    protected fun addItem(menu: JMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    protected fun addItem(menu: JPopupMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    protected fun addCopyAndPasteIfNeeded(popup: JBPopupMenu, sceneLocation: Point2D.Float, parent: BpmnElementId) {
        val renderedState = lastRenderedState(project)
        if (true == renderedState?.canCopyOrCut()) {
            addItem(popup, "Copy", COPY, ActionListener { copyToClipboard(project) })
            addItem(popup, "Cut", CUT, ActionListener { cutToClipboard(project) })
        }

        if (copyPasteActionHandler(project).hasDataToPaste()) {
            addItem(popup, "Paste", PASTE, ActionListener { pasteFromClipboard(project, sceneLocation, parent) })
        }
    }
}