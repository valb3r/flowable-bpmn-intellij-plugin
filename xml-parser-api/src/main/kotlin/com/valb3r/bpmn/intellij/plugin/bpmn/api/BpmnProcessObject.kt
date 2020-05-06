package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property

data class BpmnProcessObject(val process: BpmnProcess, val diagram: DiagramElement) {

    fun toView() : BpmnProcessObjectView {
        val elementById = mutableMapOf<String, WithId>()
        val propertiesById = mutableMapOf<String, Property>()

        elementById[process.startEvent.id] = process.startEvent
        elementById[process.endEvent.id] = process.endEvent

        process.callActivity?.forEach { fillForCallActivity(it, elementById, propertiesById) }
        process.serviceTask?.forEach { fillForServiceTask(it, elementById, propertiesById) }
        process.sequenceFlow?.forEach { fillForSequenceFlow(it, elementById, propertiesById) }
        process.exclusiveGateway?.forEach { fillForExclusiveGateway(it, elementById, propertiesById) }

        return BpmnProcessObjectView(
                process,
                elementById,
                propertiesById,
                diagram
        )
    }

    private fun fillForCallActivity(
            activity: BpmnCallActivity,
            elementById: MutableMap<String, WithId>,
            propertiesById: MutableMap<String, Property>) {
        elementById[activity.id] = activity
    }

    private fun fillForServiceTask(
            activity: BpmnServiceTask,
            elementById: MutableMap<String, WithId>,
            propertiesById: MutableMap<String, Property>) {
        elementById[activity.id] = activity
    }

    private fun fillForSequenceFlow(
            activity: BpmnSequenceFlow,
            elementById: MutableMap<String, WithId>,
            propertiesById: MutableMap<String, Property>) {
        elementById[activity.id] = activity
    }

    private fun fillForExclusiveGateway(
            activity: BpmnExclusiveGateway,
            elementById: MutableMap<String, WithId>,
            propertiesById: MutableMap<String, Property>) {
        elementById[activity.id] = activity
    }
}

data class BpmnProcessObjectView(
        val process: BpmnProcess,
        val elementById: Map<String, WithId>,
        val propertiesById: Map<String, Property>,
        val diagram: DiagramElement
)