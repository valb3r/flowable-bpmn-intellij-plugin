package com.valb3r.bpmn.intellij.plugin.camunda.parser.specialcase

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
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

    private val multiStartEvent = BpmnElementId("multiStartEvent")
    private val multiServiceTask = BpmnElementId("multiServiceTask")
    private val multiEndEvent = BpmnElementId("multiEndEvent")

    @Test
    fun `Service task (single props) with incoming-outgoing is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val startEventProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleStartEvent]!!
        val serviceTaskProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleServiceTask]!!
        val endEventProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[singleEndEvent]!!

        startEventProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeNull()
        startEventProps[PropertyType.BPMN_OUTGOING]?.shouldBeEqualTo(Property("flowFromStartEvent", listOf("flowFromStartEvent")))

        serviceTaskProps[PropertyType.BPMN_INCOMING]?.shouldBeEqualTo(Property("flowFromStartEvent", listOf("flowFromStartEvent")))
        serviceTaskProps[PropertyType.BPMN_OUTGOING]?.shouldBeEqualTo(Property("flowToEndEvent", listOf("flowToEndEvent")))

        endEventProps[PropertyType.BPMN_INCOMING]?.shouldBeEqualTo(Property("flowToEndEvent", listOf("flowToEndEvent")))
        endEventProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeNull()
    }

    @Test
    fun `Service task (single props) with incoming-outgoing is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value, "flowFromStartEvent").incoming.shouldBeEqualTo(listOf(value))} ("new incoming");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value, "flowToEndEvent").outgoing.shouldBeEqualTo(listOf(value))} ("new outgoing");
    }

    @Test
    fun `Service task (single props) with incoming-outgoing are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_INCOMING, value, "flowFromStartEvent").incoming?.shouldBeEmpty()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.BPMN_OUTGOING, value, "flowToEndEvent").outgoing?.shouldBeEmpty()} ("");
    }

    @Test
    fun `Service task (multiple props) with incoming-outgoing is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val startEventProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[multiStartEvent]!!
        val serviceTaskProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[multiServiceTask]!!
        val endEventProps = BpmnFileObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[multiEndEvent]!!

        startEventProps[PropertyType.BPMN_INCOMING]?.value?.shouldBeNull()
        startEventProps.getAll(PropertyType.BPMN_OUTGOING).shouldBeEqualTo(listOf(Property("fromStart1", listOf("fromStart1")), Property("fromStart2", listOf("fromStart2"))))

        serviceTaskProps.getAll(PropertyType.BPMN_INCOMING).shouldBeEqualTo(listOf(Property("fromStart1", listOf("fromStart1")), Property("fromStart2", listOf("fromStart2"))))
        serviceTaskProps.getAll(PropertyType.BPMN_OUTGOING).shouldBeEqualTo(listOf(Property("toEnd2", listOf("toEnd2")), Property("toEnd1", listOf("toEnd1"))))

        endEventProps.getAll(PropertyType.BPMN_INCOMING).shouldBeEqualTo(listOf(Property("toEnd2", listOf("toEnd2")), Property("toEnd1", listOf("toEnd1"))))
        endEventProps[PropertyType.BPMN_OUTGOING]?.value?.shouldBeNull()
    }

    @Test
    fun `Service task (multiple props) with incoming-outgoing is updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.BPMN_INCOMING, value, "fromStart1").incoming.shouldBeEqualTo(listOf(value, "fromStart2"))} ("new incoming");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.BPMN_OUTGOING, value, "toEnd2").outgoing.shouldBeEqualTo(listOf(value, "toEnd1"))} ("new outgoing");
    }

    @Test
    fun `Service task (multiple props) with incoming-outgoing are emptyable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.BPMN_INCOMING, value, "fromStart1").incoming?.shouldHaveSingleItem()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.BPMN_OUTGOING, value, "toEnd2").outgoing?.shouldHaveSingleItem()} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singleServiceTask, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventMultiProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiServiceTask, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiProp(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSize(2)[1]
    }
}
