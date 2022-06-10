package com.valb3r.bpmn.intellij.plugin.commons.actions.shapechange

import ShapeChange
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*

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

    fun toScriptTask(elementId: BpmnElementId): ShapeChange<BpmnScriptTask> {
        return ShapeChange(project, BpmnScriptTask::class, elementId)
    }

    fun toBusinessRuleTask(elementId: BpmnElementId): ShapeChange<BpmnBusinessRuleTask> {
        return ShapeChange(project, BpmnBusinessRuleTask::class, elementId)
    }

    fun toReceiveTask(elementId: BpmnElementId): ShapeChange<BpmnReceiveTask> {
        return ShapeChange(project, BpmnReceiveTask::class, elementId)
    }

    fun toManualTask(elementId: BpmnElementId): ShapeChange<BpmnManualTask> {
        return ShapeChange(project, BpmnManualTask::class, elementId)
    }

    fun toServiceTaskWithType(elementId: BpmnElementId, type: String): ShapeChange<out WithBpmnId> {
        val dummyBpmnServiceTask = BpmnServiceTask(BpmnElementId(""))
        return when (type) {
            "camel" -> ShapeChange(project, BpmnCamelTask::class, elementId, Pair(dummyBpmnServiceTask, "camel"))
            "http" -> ShapeChange(project, BpmnHttpTask::class, elementId, Pair(dummyBpmnServiceTask, "http"))
            "mail" -> ShapeChange(project, BpmnMailTask::class, elementId, Pair(dummyBpmnServiceTask, "mail"))
            "mule" -> ShapeChange(project, BpmnMuleTask::class, elementId, Pair(dummyBpmnServiceTask, "mule"))
            "dmn" -> ShapeChange(project, BpmnDecisionTask::class, elementId, Pair(dummyBpmnServiceTask, "dmn"))
            "shell" -> ShapeChange(project, BpmnShellTask::class, elementId, Pair(dummyBpmnServiceTask, "shell"))
            else -> {
                throw IllegalArgumentException("Cant change shape to type $type")
            }
        }
    }

}