package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateLinkCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnComplexGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
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

private const val FILE = "fullelements/full-complex-gateway.bpmn"

internal class ComplexGatewayWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("complexGatewaySingle")
    private val multiplePropElementId = BpmnElementId("complexGatewayMulti")

    @Test
    fun `Task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSingleIntermediateLinkCatchingEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("Single complex gateway")
        task.documentation.shouldBeEqualTo("Single complex gateway docs")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Task (single props) is updatable`() {
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }


    @Test
    fun `Task (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiIntermediateLinkCatchingEvent(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("Multi complex gateway")
        task.documentation.shouldBeEqualTo("Mutli complex gateway docs")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Task (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    private fun readAndUpdateSingleIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnComplexGateway {
        return readSingleIntermediateLinkCatchingEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readSingleIntermediateLinkCatchingEventSingleProp(processObject: BpmnProcessObject): BpmnComplexGateway {
        return processObject.process.body!!.complexGateway!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnComplexGateway {
        return readStartEventMultiIntermediateLinkCatchingEvent(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiplePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiIntermediateLinkCatchingEvent(processObject: BpmnProcessObject): BpmnComplexGateway {
        return processObject.process.body!!.complexGateway!!.shouldHaveSize(2)[1]
    }
}