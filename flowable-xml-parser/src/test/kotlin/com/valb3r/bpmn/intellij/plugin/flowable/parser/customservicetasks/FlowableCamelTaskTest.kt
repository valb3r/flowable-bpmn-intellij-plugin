package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnCamelTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/camel-task.bpmn20.xml"

internal class FlowableCamelTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("camelTaskid")

    @Test
    fun `Camel task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readCamelTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Camel task name")
        task.documentation.shouldBeEqualTo("Docs for camel task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.camelContext.shouldBeEqualTo("CAMEL_CTX")

        val props = BpmnFileObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.CAMEL_CONTEXT]!!.value.shouldBeEqualTo(task.camelContext)
    }

    @Test
    fun `Camel task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.CAMEL_CONTEXT, value).camelContext.shouldBeEqualTo(value)} ("NEW<>CAMEL-CTX")
    }

    @Test
    fun `Camel rule task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CAMEL_CONTEXT).camelContext.shouldBeNullOrEmpty()
    }
    
    @Test
    fun `Camel task is addable when no extension`() {
        {value: String -> readAndUpdate(PropertyType.CAMEL_CONTEXT, "custom-service-tasks/custom/camel-task-no-ext.bpmn20.xml", value).camelContext.shouldBeEqualTo(value)} ("NEW<>CAMEL-CTX")
    }

    @Test
    fun `Camel task is addable when no field`() {
        {value: String -> readAndUpdate(PropertyType.CAMEL_CONTEXT, "custom-service-tasks/custom/camel-task-no-field.bpmn20.xml", value).camelContext.shouldBeEqualTo(value)} ("NEW<>CAMEL-CTX")
    }

    @Test
    fun `Camel task is addable when no string`() {
        {value: String -> readAndUpdate(PropertyType.CAMEL_CONTEXT, "custom-service-tasks/custom/camel-task-no-string.bpmn20.xml", value).camelContext.shouldBeEqualTo(value)} ("NEW<>CAMEL-CTX")
    }

    private fun readAndSetNullString(property: PropertyType): BpmnCamelTask {
        return readCamelTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }
    
    private fun readAndUpdate(property: PropertyType, file: String, newValue: String): BpmnCamelTask {
        return readCamelTask(readAndUpdateProcess(parser, file, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnCamelTask {
        return readCamelTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnCamelTask {
        return readCamelTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readCamelTask(processObject: BpmnFileObject): BpmnCamelTask {
        return processObject.process.body!!.camelTask!!.shouldHaveSingleItem()
    }
}
