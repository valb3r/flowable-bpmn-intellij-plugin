package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import org.amshove.kluent.shouldNotBeNull
import java.nio.charset.StandardCharsets

fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

fun readAndUpdateProcess(parser: ActivitiParser, event: EventPropagatableToXml): BpmnProcessObject {
    return readAndUpdateProcess(parser, "simple-nested.bmpn20.xml", event)
}

fun readAndUpdateProcess(parser: ActivitiParser, processName: String, event: EventPropagatableToXml): BpmnProcessObject {
    val updated = parser.update(
            processName.asResource()!!,
            listOf(event)
    )

    updated.shouldNotBeNull()

    return parser.parse(updated)
}