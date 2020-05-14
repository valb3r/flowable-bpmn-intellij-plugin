package com.valb3r.bpmn.intellij.plugin.bpmn.api

import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import java.io.InputStream
import java.io.OutputStream

interface BpmnParser {

    fun parse(input: InputStream): BpmnProcessObject
    fun parse(input: String): BpmnProcessObject

    fun update(input: String, events: List<Event>): String
    fun update(input: InputStream, output: OutputStream, events: List<Event>)
}