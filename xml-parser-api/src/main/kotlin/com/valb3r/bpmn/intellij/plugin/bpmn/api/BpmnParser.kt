package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import java.io.InputStream

interface BpmnParser {

    fun parse(input: InputStream): BpmnProcessObject
    fun parse(input: String): BpmnProcessObject

    // Keeping update model simple by following:
    // https://www.jetbrains.org/intellij/sdk/docs/tutorials/editor_basics/working_with_text.html#safely-replacing-selected-text-in-the-document
    fun update(input: String, output: (String) -> Unit, events: List<Event>)
}