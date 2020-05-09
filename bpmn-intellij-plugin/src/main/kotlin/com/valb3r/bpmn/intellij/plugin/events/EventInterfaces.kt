package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

interface Event

interface LocationUpdateWithId: Event {
    val diagramElementId: DiagramElementId
    val dx: Float
    val dy: Float
}

interface PropertyUpdateWithId: Event {
    val bpmnElementId: BpmnElementId
    val property: PropertyType
}