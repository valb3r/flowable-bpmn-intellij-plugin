package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldNotBeNull
import java.nio.charset.StandardCharsets

fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

fun readAndUpdateProcess(parser: CamundaParser, event: EventPropagatableToXml): BpmnProcessObject {
    return readAndUpdateProcess(parser, "simple-nested.bpmn20.xml", event)
}

fun readAndUpdateProcess(parser: CamundaParser, processName: String, event: EventPropagatableToXml): BpmnProcessObject {
    return readAndUpdateProcess(parser, processName, listOf(event))
}

fun readAndUpdateProcess(parser: CamundaParser, processName: String, events: List<EventPropagatableToXml>): BpmnProcessObject {
    val updated = updateBpmnFile(parser, processName, events)
    return parser.parse(updated)
}

fun updateEvt(elemId: String, prop: PropertyType, newValue: String): StringValueUpdatedEvent {
    return StringValueUpdatedEvent(BpmnElementId(elemId), prop, newValue)
}

fun BpmnProcessObject.propsOf(elemId: String): PropertyTable {
    return this.toView(CamundaObjectFactory()).elemPropertiesByElementId[BpmnElementId(elemId)]!!
}
fun updateBpmnFile(parser: CamundaParser, processName: String, events: List<EventPropagatableToXml>): String {
    val updated = parser.update(
        processName.asResource()!!,
        events
    )

    updated.shouldNotBeNull()
    return updated
}