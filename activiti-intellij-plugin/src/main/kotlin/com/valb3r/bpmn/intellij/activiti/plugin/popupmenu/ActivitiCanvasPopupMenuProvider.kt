package com.valb3r.bpmn.intellij.activiti.plugin.popupmenu

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.BaseCanvasPopupMenuProvider
import java.awt.geom.Point2D
import javax.swing.JMenu
import javax.swing.JPopupMenu

class ActivitiCanvasPopupMenuProvider(project: Project) : BaseCanvasPopupMenuProvider(project) {

    override fun startEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Start events")
        menu.addNewElementItem(sceneLocation, focus, START_EVENT)
        menu.addNewElementItem(sceneLocation, focus, START_TIMER_EVENT)
        menu.addNewElementItem(sceneLocation, focus, START_SIGNAL_EVENT)
        menu.addNewElementItem(sceneLocation, focus, START_MESSAGE_EVENT)
        menu.addNewElementItem(sceneLocation, focus, START_ERROR_EVENT)
        return menu
    }

    override fun activities(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Activities")
        menu.addNewElementItem(sceneLocation, focus, USER_TASK)
        menu.addNewElementItem(sceneLocation, focus, SERVICE_TASK)
        menu.addNewElementItem(sceneLocation, focus, SCRIPT_TASK)
        menu.addNewElementItem(sceneLocation, focus, BUSINESS_RULE_TASK)
        menu.addNewElementItem(sceneLocation, focus, RECEIVE_TASK)
        menu.addNewElementItem(sceneLocation, focus, MANUAL_TASK)
        menu.addNewElementItem(sceneLocation, focus, MAIL_TASK)
        menu.addNewElementItem(sceneLocation, focus, CAMEL_TASK)
        menu.addNewElementItem(sceneLocation, focus, MULE_TASK)
        menu.addNewElementItem(sceneLocation, focus, DECISION_TASK)
        return menu
    }

    override fun structural(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Structural")
        menu.addNewElementItem(sceneLocation, focus, SUB_PROCESS)
        menu.addNewElementItem(sceneLocation, focus, EVENT_SUB_PROCESS)
        menu.addNewElementItem(sceneLocation, focus, CALL_ACTIVITY)
        return menu
    }

    override fun gateways(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Gateways")
        menu.addNewElementItem(sceneLocation, focus, EXCLUSIVE_GATEWAY)
        menu.addNewElementItem(sceneLocation, focus, PARALLEL_GATEWAY)
        menu.addNewElementItem(sceneLocation, focus, INCLUSIVE_GATEWAY)
        menu.addNewElementItem(sceneLocation, focus, EVENT_GATEWAY)
        return menu
    }

    override fun boundaryEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Boundary events")
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_ERROR_EVENT)
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_TIMER_EVENT)
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_SIGNAL_EVENT)
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_MESSAGE_EVENT)
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_CANCEL_EVENT)
        menu.addNewElementItem(sceneLocation, focus, BOUNDARY_COMPENSATION_EVENT)
        return menu
    }

    override fun intermediateCatchingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate catching events")
        menu.addNewElementItem(sceneLocation, focus, INTERMEDIATE_TIMER_CATCHING)
        menu.addNewElementItem(sceneLocation, focus, INTERMEDIATE_MESSAGE_CATCHING)
        menu.addNewElementItem(sceneLocation, focus, INTERMEDIATE_SIGNAL_CATCHING)
        return menu
    }

    override fun intermediateThrowingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("Intermediate throwing events")
        menu.addNewElementItem(sceneLocation, focus, INTERMEDIATE_NONE_THROWING)
        menu.addNewElementItem(sceneLocation, focus, INTERMEDIATE_SIGNAL_THROWING)
        return menu
    }

    override fun endEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
        val menu = JMenu("End events")
        menu.addNewElementItem(sceneLocation, focus, END_EVENT)
        menu.addNewElementItem(sceneLocation, focus, ERROR_END_EVENT)
        menu.addNewElementItem(sceneLocation, focus, CANCEL_END_EVENT)
        menu.addNewElementItem(sceneLocation, focus, TERMINATE_END_EVENT)
        return menu
    }

    override fun mutateStartEvent(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, START_EVENT)
        popup.addMutateElementItem(focus, START_TIMER_EVENT)
        popup.addMutateElementItem(focus, START_SIGNAL_EVENT)
        popup.addMutateElementItem(focus, START_ERROR_EVENT)
    }

    override fun mutateBoundaryEvents(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, BOUNDARY_ERROR_EVENT)
        popup.addMutateElementItem(focus, BOUNDARY_TIMER_EVENT)
        popup.addMutateElementItem(focus, BOUNDARY_SIGNAL_EVENT)
        popup.addMutateElementItem(focus, BOUNDARY_MESSAGE_EVENT)
        popup.addMutateElementItem(focus, BOUNDARY_CANCEL_EVENT)
        popup.addMutateElementItem(focus, BOUNDARY_COMPENSATION_EVENT)
    }

    override fun mutateTask(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, USER_TASK)
        popup.addMutateElementItem(focus, SERVICE_TASK)
        popup.addMutateElementItem(focus, SCRIPT_TASK)
        popup.addMutateElementItem(focus, BUSINESS_RULE_TASK)
        popup.addMutateElementItem(focus, RECEIVE_TASK)
        popup.addMutateElementItem(focus, MANUAL_TASK)
        popup.addMutateElementItem(focus, MAIL_TASK)
        popup.addMutateElementItem(focus, CAMEL_TASK)
        popup.addMutateElementItem(focus, MULE_TASK)
        popup.addMutateElementItem(focus, DECISION_TASK)
    }

    override fun mutateGateway(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, EXCLUSIVE_GATEWAY)
        popup.addMutateElementItem(focus, PARALLEL_GATEWAY)
        popup.addMutateElementItem(focus, INCLUSIVE_GATEWAY)
        popup.addMutateElementItem(focus, EVENT_GATEWAY)
    }

    override fun mutateStructuralElement(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, SUB_PROCESS)
        popup.addMutateElementItem(focus, EVENT_SUB_PROCESS)
        popup.addMutateElementItem(focus, CALL_ACTIVITY)
    }

    override fun mutateIntermediateThrowingEvent(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, INTERMEDIATE_NONE_THROWING)
        popup.addMutateElementItem(focus, INTERMEDIATE_SIGNAL_THROWING)
    }

    override fun mutateIntermediateCatchingEvent(popup: JPopupMenu, focus: BpmnElementId) {
        popup.addMutateElementItem(focus, INTERMEDIATE_TIMER_CATCHING)
        popup.addMutateElementItem(focus, INTERMEDIATE_MESSAGE_CATCHING)
        popup.addMutateElementItem(focus, INTERMEDIATE_SIGNAL_CATCHING)
    }

    override fun mutateEndEvent(popup: JPopupMenu, focus: BpmnElementId){
        popup.addMutateElementItem(focus, END_EVENT)
        popup.addMutateElementItem(focus, ERROR_END_EVENT)
        popup.addMutateElementItem(focus, CANCEL_END_EVENT)
        popup.addMutateElementItem(focus, TERMINATE_END_EVENT)
    }
}