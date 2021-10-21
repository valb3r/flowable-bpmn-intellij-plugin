package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateLinkCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

private const val FILE = "fullelements/full-link-intermediate-catch-event.bpmn"

internal class IntermediateLinkCatchingEventWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("linkIntermediateCatchEventSingle")
    private val multiplePropElementId = BpmnElementId("linkIntermediateCatchEventMulti")

    @Test
    fun `Intermediate link catching event (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSingleIntermediateLinkCatchingEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("Link intermediate cache event")
        task.documentation.shouldBeEqualTo("A link intermediate catch event")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Intermediate link catching event (single props) is updatable`() {
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }


    @Test
    fun `Intermediate link catching event (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiIntermediateLinkCatchingEvent(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("Link intermediate cache event")
        task.documentation.shouldBeEqualTo("A link intermediate catch event")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Intermediate link catching event (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    private fun readAndUpdateSingleIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnIntermediateLinkCatchingEvent {
        return readSingleIntermediateLinkCatchingEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readSingleIntermediateLinkCatchingEventSingleProp(processObject: BpmnProcessObject): BpmnIntermediateLinkCatchingEvent {
        return processObject.process.body!!.intermediateLinkCatchingEvent!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnIntermediateLinkCatchingEvent {
        return readStartEventMultiIntermediateLinkCatchingEvent(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiplePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiIntermediateLinkCatchingEvent(processObject: BpmnProcessObject): BpmnIntermediateLinkCatchingEvent {
        return processObject.process.body!!.intermediateLinkCatchingEvent!!.shouldHaveSize(2)[1]
    }
}