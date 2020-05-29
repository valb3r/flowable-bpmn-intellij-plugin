package com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu

import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateConditionalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JMenu
import kotlin.reflect.KClass


private val popupMenuProvider = AtomicReference<CanvasPopupMenuProvider>()

fun popupMenuProvider(): CanvasPopupMenuProvider {
    return popupMenuProvider.updateAndGet {
        if (null == it) {
            return@updateAndGet CanvasPopupMenuProvider()
        }

        return@updateAndGet it
    }
}

private fun <T: WithBpmnId> newShapeElement(sceneLocation: Point2D.Float, forObject: T): ShapeElement {
    val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, forObject)

    return templateShape.copy(
            bounds = BoundsElement(
                    sceneLocation.x,
                    sceneLocation.y,
                    templateShape.bounds.width,
                    templateShape.bounds.height
            )
    )
}

class CanvasPopupMenuProvider {

    // Events
    // Start
    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
    private val START_CONDITIONAL_EVENT = IconLoader.getIcon("/icons/popupmenu/conditional-start-event.png")
    private val START_MESSAGE_EVENT = IconLoader.getIcon("/icons/popupmenu/message-start-event.png")
    private val START_ERROR_EVENT = IconLoader.getIcon("/icons/popupmenu/error-start-event.png")
    private val START_ESCALATION_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-start-event.png")
    private val START_SIGNAL_EVENT = IconLoader.getIcon("/icons/popupmenu/signal-start-event.png")
    private val START_TIMER_EVENT = IconLoader.getIcon("/icons/popupmenu/timer-start-event.png")
    // End
    private val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png")
    private val CANCEL_END_EVENT = IconLoader.getIcon("/icons/popupmenu/cancel-end-event.png")
    private val ERROR_END_EVENT = IconLoader.getIcon("/icons/popupmenu/error-end-event.png")
    private val ESCALATION_END_EVENT = IconLoader.getIcon("/icons/popupmenu/escalation-end-event.png")
    private val TERMINATE_END_EVENT = IconLoader.getIcon("/icons/popupmenu/terminate-end-event.png")
    // Intermediate events
    // Catch
    private val INTERMEDIATE_TIMER_CATCHING = IconLoader.getIcon("/icons/popupmenu/timer-catch-event.png")
    private val INTERMEDIATE_MESSAGE_CATCHING = IconLoader.getIcon("/icons/popupmenu/message-catch-event.png")
    private val INTERMEDIATE_SIGNAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/signal-catch-event.png")
    private val INTERMEDIATE_CONDITIONAL_CATCHING = IconLoader.getIcon("/icons/popupmenu/conditional-catch-event.png")
    // Throw
    private val INTERMEDIATE_NONE_THROWING = IconLoader.getIcon("/icons/popupmenu/none-throw-event.png")
    private val INTERMEDIATE_SIGNAL_THROWING = IconLoader.getIcon("/icons/popupmenu/signal-throw-event.png")
    private val INTERMEDIATE_ESCALATION_THROWING = IconLoader.getIcon("/icons/popupmenu/escalation-throw-event.png")

    // Service-task alike
    private val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png")
    private val USER_TASK = IconLoader.getIcon("/icons/popupmenu/user-task.png")
    private val SCRIPT_TASK = IconLoader.getIcon("/icons/popupmenu/script-task.png")
    private val BUSINESS_RULE_TASK = IconLoader.getIcon("/icons/popupmenu/business-rule-task.png")
    private val RECEIVE_TASK = IconLoader.getIcon("/icons/popupmenu/receive-task.png")
    private val CAMEL_TASK = IconLoader.getIcon("/icons/popupmenu/camel-task.png")
    private val HTTP_TASK = IconLoader.getIcon("/icons/popupmenu/http-task.png")
    private val MULE_TASK = IconLoader.getIcon("/icons/popupmenu/mule-task.png")
    private val DECISION_TASK = IconLoader.getIcon("/icons/popupmenu/decision-task.png")
    private val SHELL_TASK = IconLoader.getIcon("/icons/popupmenu/shell-task.png")

    // Sub process alike
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    private val SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/subprocess.png")
    private val ADHOC_SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/adhoc-subprocess.png")

    // Gateway
    private val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png")
    private val PARALLEL_GATEWAY = IconLoader.getIcon("/icons/popupmenu/parallel-gateway.png")
    private val INCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/inclusive-gateway.png")
    private val EVENT_GATEWAY = IconLoader.getIcon("/icons/popupmenu/event-gateway.png")

    fun popupMenu(sceneLocation: Point2D.Float): JBPopupMenu {
        val popup = JBPopupMenu()
        popup.add(startEvents(sceneLocation))
        popup.add(activities(sceneLocation))
        popup.add(structural(sceneLocation))
        popup.add(gateways(sceneLocation))
        popup.add(intermediateCatchingEvents(sceneLocation))
        popup.add(intermediateThrowingEvents(sceneLocation))
        popup.add(endEvents(sceneLocation))
        return popup
    }

    private fun startEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Start events")
        addItem(menu, "Start event", START_EVENT, ShapeCreator(BpmnStartEvent::class, sceneLocation))
        addItem(menu, "Start conditional event", START_CONDITIONAL_EVENT, ShapeCreator(BpmnStartConditionalEvent::class, sceneLocation))
        addItem(menu, "Start message event", START_MESSAGE_EVENT, ShapeCreator(BpmnStartMessageEvent::class, sceneLocation))
        addItem(menu, "Start error event", START_ERROR_EVENT, ShapeCreator(BpmnStartErrorEvent::class, sceneLocation))
        addItem(menu, "Start escalation event", START_ESCALATION_EVENT, ShapeCreator(BpmnStartEscalationEvent::class, sceneLocation))
        addItem(menu, "Start signal event", START_SIGNAL_EVENT, ShapeCreator(BpmnStartSignalEvent::class, sceneLocation))
        addItem(menu, "Start timer event", START_TIMER_EVENT, ShapeCreator(BpmnStartTimerEvent::class, sceneLocation))
        return menu
    }

    private fun activities(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Activities")
        addItem(menu, "User task", USER_TASK, ShapeCreator(BpmnUserTask::class, sceneLocation))
        addItem(menu, "Service task", SERVICE_TASK, ShapeCreator(BpmnServiceTask::class, sceneLocation))
        addItem(menu, "Script task", SCRIPT_TASK, ShapeCreator(BpmnScriptTask::class, sceneLocation))
        addItem(menu, "Business rule task", BUSINESS_RULE_TASK, ShapeCreator(BpmnBusinessRuleTask::class, sceneLocation))
        addItem(menu, "Receive task", RECEIVE_TASK, ShapeCreator(BpmnReceiveTask::class, sceneLocation))
        addItem(menu, "Camel task", CAMEL_TASK, ShapeCreator(BpmnCamelTask::class, sceneLocation))
        addItem(menu, "Http task", HTTP_TASK, ShapeCreator(BpmnHttpTask::class, sceneLocation))
        addItem(menu, "Mule task", MULE_TASK, ShapeCreator(BpmnMuleTask::class, sceneLocation))
        addItem(menu, "Decision task", DECISION_TASK, ShapeCreator(BpmnDecisionTask::class, sceneLocation))
        addItem(menu, "Shell task", SHELL_TASK, ShapeCreator(BpmnShellTask::class, sceneLocation))
        return menu
    }

    private fun structural(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Structural")
        addItem(menu, "Sub process", SUB_PROCESS, ShapeCreator(BpmnSubProcess::class, sceneLocation))
        addItem(menu, "Call activity", CALL_ACTIVITY, ShapeCreator(BpmnCallActivity::class, sceneLocation))
        addItem(menu, "Adhoc sub process", ADHOC_SUB_PROCESS, ShapeCreator(BpmnAdHocSubProcess::class, sceneLocation))
        return menu
    }

    private fun gateways(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Gateways")
        addItem(menu, "Exclusive gateway", EXCLUSIVE_GATEWAY, ShapeCreator(BpmnExclusiveGateway::class, sceneLocation))
        addItem(menu, "Parallel gateway", PARALLEL_GATEWAY, ShapeCreator(BpmnParallelGateway::class, sceneLocation))
        addItem(menu, "Inclusive gateway", INCLUSIVE_GATEWAY, ShapeCreator(BpmnInclusiveGateway::class, sceneLocation))
        addItem(menu, "Event gateway", EVENT_GATEWAY, ShapeCreator(BpmnEventGateway::class, sceneLocation))
        return menu
    }

    private fun intermediateCatchingEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Intermediate catching events")
        addItem(menu, "Intermediate timer catching event", INTERMEDIATE_TIMER_CATCHING, ShapeCreator(BpmnIntermediateTimerCatchingEvent::class, sceneLocation))
        addItem(menu, "Intermediate message catching event", INTERMEDIATE_MESSAGE_CATCHING, ShapeCreator(BpmnIntermediateMessageCatchingEvent::class, sceneLocation))
        addItem(menu, "Intermediate signal catching event", INTERMEDIATE_SIGNAL_CATCHING, ShapeCreator(BpmnIntermediateSignalCatchingEvent::class, sceneLocation))
        addItem(menu, "Intermediate conditional catching event", INTERMEDIATE_CONDITIONAL_CATCHING, ShapeCreator(BpmnIntermediateConditionalCatchingEvent::class, sceneLocation))
        return menu
    }

    private fun intermediateThrowingEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Intermediate throwing events")
        addItem(menu, "Intermediate none throwing event", INTERMEDIATE_NONE_THROWING, ShapeCreator(BpmnIntermediateNoneThrowingEvent::class, sceneLocation))
        addItem(menu, "Intermediate signal throwing event", INTERMEDIATE_SIGNAL_THROWING, ShapeCreator(BpmnIntermediateSignalThrowingEvent::class, sceneLocation))
        addItem(menu, "Intermediate escalation throwing event", INTERMEDIATE_ESCALATION_THROWING, ShapeCreator(BpmnIntermediateEscalationThrowingEvent::class, sceneLocation))
        return menu
    }

    private fun endEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("End events")
        addItem(menu, "End event", END_EVENT, ShapeCreator(BpmnEndEvent::class, sceneLocation))
        addItem(menu, "End error event", ERROR_END_EVENT, ShapeCreator(BpmnEndErrorEvent::class, sceneLocation))
        addItem(menu, "End escalation event", ESCALATION_END_EVENT, ShapeCreator(BpmnEndEscalationEvent::class, sceneLocation))
        addItem(menu, "End cancel event", CANCEL_END_EVENT, ShapeCreator(BpmnEndCancelEvent::class, sceneLocation))
        addItem(menu, "End terminate event", TERMINATE_END_EVENT, ShapeCreator(BpmnEndTerminateEvent::class, sceneLocation))
        return menu
    }

    private fun addItem(menu: JMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    private class ShapeCreator<T : WithBpmnId> (private val clazz: KClass<T>, private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val newObject = newElementsFactory().newBpmnObject(clazz)
            val shape = newShapeElement(sceneLocation, newObject)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(newObject, shape, newElementsFactory().propertiesOf(newObject))
            )
        }
    }
}