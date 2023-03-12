package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnMuleTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/mule-task.bpmn20.xml"

internal class ActivityMuleTaskTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("muleTaskId")

    @Test
    fun `Mail task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readMuleTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Mule task name")
        task.documentation.shouldBeEqualTo("Docs for mule task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.endpointUrl.shouldBeEqualTo("http://example.com")
        task.language.shouldBeEqualTo("JAVA")
        task.payloadExpression.shouldBeEqualTo("\${foo.bar}")
        task.resultVariableCdata.shouldBeEqualTo("RESULT")

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(ActivitiObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.ENDPOINT_URL]!!.value.shouldBeEqualTo(task.endpointUrl)
        props[PropertyType.LANGUAGE]!!.value.shouldBeEqualTo(task.language)
        props[PropertyType.PAYLOAD_EXPRESSION]!!.value.shouldBeEqualTo(task.payloadExpression)
        props[PropertyType.RESULT_VARIABLE_CDATA]!!.value.shouldBeEqualTo(task.resultVariableCdata)
    }

    @Test
    fun `Mail task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.ENDPOINT_URL, value).endpointUrl.shouldBeEqualTo(value)} ("http://example.com/another");
        {value: String -> readAndUpdate(PropertyType.LANGUAGE, value).language.shouldBeEqualTo(value)} ("Unknown language");
        {value: String -> readAndUpdate(PropertyType.PAYLOAD_EXPRESSION, value).payloadExpression.shouldBeEqualTo(value)} ("val aaa = 1");
        {value: String -> readAndUpdate(PropertyType.RESULT_VARIABLE_CDATA, value).resultVariableCdata.shouldBeEqualTo(value)} ("RESULT_VAR_999")
    }

    @Test
    fun `Mule task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ENDPOINT_URL).endpointUrl.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.LANGUAGE).language.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.PAYLOAD_EXPRESSION).payloadExpression.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.RESULT_VARIABLE_CDATA).resultVariableCdata.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnMuleTask {
        return readMuleTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnMuleTask {
        return readMuleTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnMuleTask {
        return readMuleTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readMuleTask(processObject: BpmnFileObject): BpmnMuleTask {
        return processObject.processes[0].body!!.muleTask!!.shouldHaveSingleItem()
    }
}
