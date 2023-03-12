package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import org.amshove.kluent.shouldNotBeNull
import java.nio.charset.StandardCharsets

fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

fun readAndUpdateProcess(parser: ActivitiParser, event: EventPropagatableToXml): BpmnFileObject {
    return readAndUpdateProcess(parser, "simple-nested.bpmn20.xml", event)
}

fun readAndUpdateProcess(parser: ActivitiParser, processName: String, event: EventPropagatableToXml): BpmnFileObject {
    return readAndUpdateProcess(parser, processName, listOf(event))
}

fun readAndUpdateProcess(parser: ActivitiParser, processName: String, events: List<EventPropagatableToXml>): BpmnFileObject {
    val updated = updateBpmnFile(parser, processName, events)
    return parser.parse(updated)
}

fun updateBpmnFile(parser: ActivitiParser, processName: String, events: List<EventPropagatableToXml>): String {
    val updated = parser.update(
        processName.asResource()!!,
        events
    )

    updated.shouldNotBeNull()
    return updated
}
