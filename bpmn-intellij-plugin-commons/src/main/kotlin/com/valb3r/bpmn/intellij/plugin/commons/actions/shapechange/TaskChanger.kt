package com.valb3r.bpmn.intellij.plugin.commons.actions.shapechange

import ShapeChange
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask

class TaskChanger(val project: Project) {
    fun toTask(elementId: BpmnElementId): ShapeChange<BpmnTask> {
           return ShapeChange(project, BpmnTask::class, elementId)
    }

    fun toServiceTask(elementId: BpmnElementId): ShapeChange<BpmnServiceTask> {
            return ShapeChange(project, BpmnServiceTask::class, elementId)
    }

    fun toUsertask(elementId: BpmnElementId): ShapeChange<BpmnUserTask> {
            return ShapeChange(project, BpmnUserTask::class, elementId)
    }

}