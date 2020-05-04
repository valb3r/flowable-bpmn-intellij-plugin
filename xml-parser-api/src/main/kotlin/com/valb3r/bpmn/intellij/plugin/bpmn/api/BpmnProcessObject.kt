package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement

data class BpmnProcessObject(val process: BpmnProcess, val diagram: DiagramElement) {

    fun toView() : BpmnProcessObjectView {
        val elementById = mutableMapOf<String, WithId>()

        elementById[process.startEvent.id] = process.startEvent
        elementById[process.endEvent.id] = process.endEvent

        process.callActivity?.forEach { elementById[it.id] = it }
        process.serviceTask?.forEach { elementById[it.id] = it }
        process.sequenceFlow?.forEach { elementById[it.id] = it }
        process.exclusiveGateway?.forEach { elementById[it.id] = it }

        return BpmnProcessObjectView(
                process,
                elementById,
                diagram
        )
    }
}

data class BpmnProcessObjectView(
        val process: BpmnProcess,
        val elementById: Map<String, WithId>,
        val diagram: DiagramElement
)