package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

private const val FILE = "fullelements/full-send-task.bpmn"

internal class SendTaskWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("fullSendTaskSingle")
    private val multiplePropElementId = BpmnElementId("fullSendTaskMulti")

    @Test
    fun `Send task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSingleIntermediateLinkCatchingEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("Single send task")
        task.documentation.shouldBeEqualTo("Single send task docs")

        val props = BpmnFileObject(processObject.processes, processObject.collaborations, processObject.diagram).toView(CamundaObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Send task (single props) is updatable`() {
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSingleIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }


    @Test
    fun `Send task (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiIntermediateLinkCatchingEvent(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("Multi send task")
        task.documentation.shouldBeEqualTo("Multi send task docs")

        val props = BpmnFileObject(processObject.processes, processObject.collaborations, processObject.diagram).toView(CamundaObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `Send task (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropIntermediateLinkCatchingEvent(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    private fun readAndUpdateSingleIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnSendTask {
        return readSingleIntermediateLinkCatchingEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readSingleIntermediateLinkCatchingEventSingleProp(processObject: BpmnFileObject): BpmnSendTask {
        return processObject.processes[0].body!!.sendTask!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropIntermediateLinkCatchingEvent(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnSendTask {
        return readStartEventMultiIntermediateLinkCatchingEvent(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiplePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiIntermediateLinkCatchingEvent(processObject: BpmnFileObject): BpmnSendTask {
        return processObject.processes[0].body!!.sendTask!!.shouldHaveSize(2)[1]
    }
}
