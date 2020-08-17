package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml

interface BpmnParser {

    fun parse(input: String): BpmnProcessObject

    // Keeping update model simple by following:
    // https://www.jetbrains.org/intellij/sdk/docs/tutorials/editor_basics/working_with_text.html#safely-replacing-selected-text-in-the-document
    fun update(input: String, events: List<EventPropagatableToXml>): String
}