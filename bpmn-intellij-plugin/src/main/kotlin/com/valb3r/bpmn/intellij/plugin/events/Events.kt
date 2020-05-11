package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

data class StringValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, val newValue: String): PropertyUpdateWithId

data class BooleanValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, val newValue: Boolean): PropertyUpdateWithId

data class DraggedToEvent(override val diagramElementId: DiagramElementId, override val dx: Float, override val dy: Float): LocationUpdateWithId

data class NewChildElement(override val parentElementId: DiagramElementId, override val x: Float, override val y: Float): NewChildElementWithId

data class CommittedToFile(val eventCount: Int): Event