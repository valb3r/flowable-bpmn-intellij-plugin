package com.valb3r.bpmn.intellij.plugin.camunda.parser.specialcase

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
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

    private val singleStartEvent = BpmnElementId("singleStartEvent")
    private val singleServiceTask = BpmnElementId("singleServiceTask")
    private val singleEndEvent = BpmnElementId("singleEndEvent")

    @Test
    fun `Service task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val startEventProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleStartEvent]!!
        val serviceTaskProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleServiceTask]!!
        val endEventProps = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleEndEvent]!!

        startEventProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeNull()
        startEventProps[PropertyType.BPMN_OUTGOING]?.shouldBeEqualTo(Property("flowFromStartEvent", null))

        serviceTaskProps[PropertyType.BPMN_INCOMING]?.shouldBeEqualTo(Property("flowFromStartEvent", null))
        serviceTaskProps[PropertyType.BPMN_OUTGOING]?.shouldBeEqualTo(Property("flowToEndEvent", null))

        endEventProps[PropertyType.BPMN_INCOMING]?.shouldBeEqualTo(Property("flowToEndEvent", null))
        endEventProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeNull()
    }

    @Test
    fun `Service task (single props) is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value).incoming.shouldBeEqualTo(listOf(value))} ("new incoming");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value).outgoing.shouldBeEqualTo(listOf(value))} ("new outgoing");
    }

    @Test
    fun `Service task (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value).incoming?.shouldBeEmpty()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value).outgoing?.shouldBeEmpty()} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singleServiceTask, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSize(2)[0]
    }
}