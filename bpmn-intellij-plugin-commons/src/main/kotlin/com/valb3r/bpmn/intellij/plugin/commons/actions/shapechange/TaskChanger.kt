package com.valb3r.bpmn.intellij.plugin.commons.actions.shapechange

import ShapeChange
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

class TaskChanger(val project: Project) {
    fun toTask(elementId: BpmnElementId, rect: Rectangle2D.Float): ShapeChange<BpmnTask> {
        currentCanvas(project).let { canvas ->
           return ShapeChange(project, BpmnTask::class, elementId, rect, canvas.parentableElementAt(Point2D.Float(rect.x, rect.y)))
        }
    }
}