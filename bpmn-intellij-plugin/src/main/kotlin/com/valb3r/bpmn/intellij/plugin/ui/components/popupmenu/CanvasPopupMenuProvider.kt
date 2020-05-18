package com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu

import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
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

class CanvasPopupMenuProvider {

    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
    private val USER_TASK = IconLoader.getIcon("/icons/popupmenu/user-task.png")
    private val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png")
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
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
        addItem(menu, "Service task", SERVICE_TASK, NewServiceTask(sceneLocation))
        addItem(menu, "User task", USER_TASK, NewUserTask(sceneLocation))
        return menu
    }

    private fun structural(sceneLocation: Point2D.Float): JMenu {
        val menu = JMenu("Structural")
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
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, startEvent)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(startEvent, shape, newElementsFactory().propertiesOf(startEvent))
            )
        }
    }

    private class NewServiceTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val serviceTask = newElementsFactory().newBpmnObject(BpmnServiceTask::class)
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, serviceTask)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(serviceTask, shape, newElementsFactory().propertiesOf(serviceTask))
            )
        }
    }

    private class NewUserTask(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val userTask = newElementsFactory().newBpmnObject(BpmnUserTask::class)
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, userTask)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(userTask, shape, newElementsFactory().propertiesOf(userTask))
            )
        }
    }

    private class NewCallActivity(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val callActivity = newElementsFactory().newBpmnObject(BpmnCallActivity::class)
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, callActivity)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(callActivity, shape, newElementsFactory().propertiesOf(callActivity))
            )
        }
    }


    private class NewExclusiveGateway(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val exclusiveGateway = newElementsFactory().newBpmnObject(BpmnExclusiveGateway::class)
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, exclusiveGateway)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(exclusiveGateway, shape, newElementsFactory().propertiesOf(exclusiveGateway))
            )
        }
    }

    private class NewEndEvent(private val sceneLocation: Point2D.Float): ActionListener {

        override fun actionPerformed(e: ActionEvent?) {
            val endEvent = newElementsFactory().newBpmnObject(BpmnEndEvent::class)
            val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, endEvent)

            val shape = templateShape.copy(
                    bounds = BoundsElement(
                            sceneLocation.x,
                            sceneLocation.y,
                            templateShape.bounds.width,
                            templateShape.bounds.height
                    )
            )

            updateEventsRegistry().addObjectEvent(
                    BpmnShapeObjectAddedEvent(endEvent, shape, newElementsFactory().propertiesOf(endEvent))
            )
        }
    }
}