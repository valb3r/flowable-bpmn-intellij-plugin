package com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu

import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
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

    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
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
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    private val SUB_PROCESS = IconLoader.getIcon("/icons/popupmenu/subprocess.png")
    private val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png")
    private val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png")

    fun popupMenu(sceneLocation: Point2D.Float): JBPopupMenu {
        val popup = JBPopupMenu()
        popup.add(startEvents(sceneLocation))
        popup.add(activities(sceneLocation))
        popup.add(structural(sceneLocation))
        popup.add(gateways(sceneLocation))
        popup.add(endEvents(sceneLocation))
        return popup
    }

    private fun startEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Start events")
        addItem(menu, "Start event", START_EVENT, NewStartEvent(sceneLocation))
        return menu
    }

    private fun activities(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Activities")
        addItem(menu, "User task", USER_TASK, NewUserTask(sceneLocation))
        addItem(menu, "Service task", SERVICE_TASK, NewServiceTask(sceneLocation))
        addItem(menu, "Script task", SCRIPT_TASK, NewScriptTask(sceneLocation))
        addItem(menu, "Business rule task", BUSINESS_RULE_TASK, NewBusinessRuleTask(sceneLocation))
        addItem(menu, "Receive task", RECEIVE_TASK, NewReceiveTask(sceneLocation))
        addItem(menu, "Camel task", CAMEL_TASK, NewCamelTask(sceneLocation))
        addItem(menu, "Http task", HTTP_TASK, NewHttpTask(sceneLocation))
        addItem(menu, "Mule task", MULE_TASK, NewMuleTask(sceneLocation))
        addItem(menu, "Decision task", DECISION_TASK, NewDecisionTask(sceneLocation))
        addItem(menu, "Shell task", SHELL_TASK, NewShellTask(sceneLocation))
        return menu
    }

    private fun structural(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Structural")
        addItem(menu, "Sub process", SUB_PROCESS, NewSubProcess(sceneLocation))
        addItem(menu, "Call activity", CALL_ACTIVITY, NewCallActivity(sceneLocation))
        return menu
    }

    private fun gateways(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Gateways")
        addItem(menu, "Exclusive gateway", EXCLUSIVE_GATEWAY, NewExclusiveGateway(sceneLocation))
        return menu
    }

    private fun endEvents(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("End events")
        addItem(menu, "End event", END_EVENT, NewEndEvent(sceneLocation))
        return menu
    }

    private fun addItem(menu: JMenu, text: String, icon: Icon, listener: ActionListener) {
        val item = JBMenuItem(text, icon)
        item.addActionListener(listener)
        menu.add(item)
    }

    private class NewStartEvent(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val startEvent = newElementsFactory().newBpmnObject(BpmnStartEvent::class)
            val shape = newShapeElement(sceneLocation, startEvent)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(startEvent, shape, newElementsFactory().propertiesOf(startEvent))
            )
        }
    }

    private class NewServiceTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val serviceTask = newElementsFactory().newBpmnObject(BpmnServiceTask::class)
            val shape = newShapeElement(sceneLocation, serviceTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(serviceTask, shape, newElementsFactory().propertiesOf(serviceTask))
            )
        }
    }

    private class NewScriptTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val scriptTask = newElementsFactory().newBpmnObject(BpmnScriptTask::class)
            val shape = newShapeElement(sceneLocation, scriptTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(scriptTask, shape, newElementsFactory().propertiesOf(scriptTask))
            )
        }
    }

    private class NewBusinessRuleTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val businessRuleTask = newElementsFactory().newBpmnObject(BpmnBusinessRuleTask::class)
            val shape = newShapeElement(sceneLocation, businessRuleTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(businessRuleTask, shape, newElementsFactory().propertiesOf(businessRuleTask))
            )
        }
    }

    private class NewUserTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val userTask = newElementsFactory().newBpmnObject(BpmnUserTask::class)
            val shape = newShapeElement(sceneLocation, userTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(userTask, shape, newElementsFactory().propertiesOf(userTask))
            )
        }
    }

    private class NewReceiveTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val receiveTask = newElementsFactory().newBpmnObject(BpmnReceiveTask::class)
            val shape = newShapeElement(sceneLocation, receiveTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(receiveTask, shape, newElementsFactory().propertiesOf(receiveTask))
            )
        }
    }

    private class NewCamelTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val camelTask = newElementsFactory().newBpmnObject(BpmnCamelTask::class)
            val shape = newShapeElement(sceneLocation, camelTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(camelTask, shape, newElementsFactory().propertiesOf(camelTask))
            )
        }
    }

    private class NewHttpTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val httpTask = newElementsFactory().newBpmnObject(BpmnHttpTask::class)
            val shape = newShapeElement(sceneLocation, httpTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(httpTask, shape, newElementsFactory().propertiesOf(httpTask))
            )
        }
    }

    private class NewMuleTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val httpTask = newElementsFactory().newBpmnObject(BpmnMuleTask::class)
            val shape = newShapeElement(sceneLocation, httpTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(httpTask, shape, newElementsFactory().propertiesOf(httpTask))
            )
        }
    }

    private class NewDecisionTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val httpTask = newElementsFactory().newBpmnObject(BpmnDecisionTask::class)
            val shape = newShapeElement(sceneLocation, httpTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(httpTask, shape, newElementsFactory().propertiesOf(httpTask))
            )
        }
    }

    private class NewShellTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val httpTask = newElementsFactory().newBpmnObject(BpmnShellTask::class)
            val shape = newShapeElement(sceneLocation, httpTask)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(httpTask, shape, newElementsFactory().propertiesOf(httpTask))
            )
        }
    }

    private class NewSubProcess(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val subProcess = newElementsFactory().newBpmnObject(BpmnSubProcess::class)
            val shape = newShapeElement(sceneLocation, subProcess)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(subProcess, shape, newElementsFactory().propertiesOf(subProcess))
            )
        }
    }

    private class NewCallActivity(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val callActivity = newElementsFactory().newBpmnObject(BpmnCallActivity::class)
            val shape = newShapeElement(sceneLocation, callActivity)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(callActivity, shape, newElementsFactory().propertiesOf(callActivity))
            )
        }
    }


    private class NewExclusiveGateway(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val exclusiveGateway = newElementsFactory().newBpmnObject(BpmnExclusiveGateway::class)
            val shape = newShapeElement(sceneLocation, exclusiveGateway)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(exclusiveGateway, shape, newElementsFactory().propertiesOf(exclusiveGateway))
            )
        }
    }

    private class NewEndEvent(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val endEvent = newElementsFactory().newBpmnObject(BpmnEndEvent::class)
            val shape = newShapeElement(sceneLocation, endEvent)

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(endEvent, shape, newElementsFactory().propertiesOf(endEvent))
            )
        }
    }
}