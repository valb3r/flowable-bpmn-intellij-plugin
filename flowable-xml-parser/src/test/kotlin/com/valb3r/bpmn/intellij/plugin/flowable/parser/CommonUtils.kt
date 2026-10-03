package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldNotBeNull
import java.nio.charset.StandardCharsets

fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

fun readAndUpdateProcess(parser: FlowableParser, event: EventPropagatableToXml): BpmnProcessObject {
    return readAndUpdateProcess(parser, "simple-nested.bpmn20.xml", event)
}

fun readAndUpdateProcess(parser: FlowableParser, processName: String, event: EventPropagatableToXml): BpmnProcessObject {
    return readAndUpdateProcess(parser, processName, listOf(event))
}

fun readAndUpdateProcess(parser: FlowableParser, processName: String, events: List<EventPropagatableToXml>): BpmnProcessObject {
    val updated = updateBpmnFile(parser, processName, events)
    return parser.parse(updated)
}

fun updateEvt(elemId: String, prop: PropertyType, newValue: String): StringValueUpdatedEvent {
    return StringValueUpdatedEvent(BpmnElementId(elemId), prop, newValue)
}

fun BpmnProcessObject.propsOf(elemId: String): PropertyTable {
    return this.toView(FlowableObjectFactory()).elemPropertiesByElementId[BpmnElementId(elemId)]!!
}

fun updateBpmnFile(parser: FlowableParser, processName: String, events: List<EventPropagatableToXml>): String {
    val updated = parser.update(
        processName.asResource()!!,
        events
    )

    updated.shouldNotBeNull()
    return updated
}