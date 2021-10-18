package com.valb3r.bpmn.intellij.plugin.camunda.parser.specialcase

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "specialcase/bpmn-incoming-outgoing-special.bpmn"

internal class SpecialCaseIncomingOutgoingIsParseable {

    private val parser = CamundaParser()
    private val startEvent = BpmnElementId("startEvent")
    private val serviceTask = BpmnElementId("serviceTask")
    private val endEvent = BpmnElementId("endEvent")

    @Test
    fun `Service task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val startEventProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[startEvent]!!
        val serviceTaskProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[serviceTask]!!
        val endEventProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[endEvent]!!

        startEventProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeNull()
        startEventProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeEqualTo("flowFromStartEvent")

        serviceTaskProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeEqualTo("flowFromStartEvent")
        serviceTaskProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeEqualTo("flowToEndEvent")

        endEventProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeEqualTo("flowToEndEvent")
        endEventProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeNull()

    }

    @Test
    fun `Service task (single props) is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value).incoming.shouldBeEqualTo(value)} ("new incoming");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value).outgoing.shouldBeEqualTo(value)} ("new outgoing");
    }

    @Test
    fun `Service task (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value).incoming.shouldBeEqualTo(value)} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value).outgoing.shouldBeEqualTo(value)} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(serviceTask, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
    }
}