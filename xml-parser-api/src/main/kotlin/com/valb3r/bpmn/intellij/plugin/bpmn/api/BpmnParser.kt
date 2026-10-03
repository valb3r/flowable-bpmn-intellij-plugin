package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement

interface BpmnParser {

    fun parse(input: String): BpmnProcessObject
    fun validateForErrors(input: String): String?
    fun validateForWarnings(input: String): String?

    // Keeping update model simple by following:
    // https://www.jetbrains.org/intellij/sdk/docs/tutorials/editor_basics/working_with_text.html#safely-replacing-selected-text-in-the-document
    fun update(input: String, events: List<EventPropagatableToXml>): String

    fun updateDiagram(input: String, diagrams: List<DiagramElement>): String
}
