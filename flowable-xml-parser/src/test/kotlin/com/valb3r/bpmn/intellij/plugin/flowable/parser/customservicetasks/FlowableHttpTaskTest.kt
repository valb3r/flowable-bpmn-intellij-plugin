package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnHttpTask
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

private const val FILE = "custom-service-tasks/http-task.bpmn20.xml"

internal class FlowableHttpTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("httpTaskId")

    @Test
    fun `Http task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readHttpTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Http task name")
        task.documentation.shouldBeEqualTo("Docs for http task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.requestMethod.shouldBeEqualTo("GET")
        task.requestUrl.shouldBeEqualTo("http://example.com")
        task.requestHeaders.shouldBeEqualTo("X-Request-ID")
        task.requestBody.shouldBeEqualTo("""
            {
               "message": "Hello"
               "tag": "<tag>"
            }
        """.trimIndent())
        task.requestBodyEncoding.shouldBeEqualTo("UTF-8")
        task.requestTimeout.shouldBeEqualTo("30S")
        task.disallowRedirects.shouldBeEqualTo(true)
        task.failStatusCodes.shouldBeEqualTo("404")
        task.handleStatusCodes.shouldBeEqualTo("200")
        task.responseVariableName.shouldBeEqualTo("RESPONSE")
        task.ignoreException.shouldBeEqualTo("ignoreMe")
        task.saveRequestVariables.shouldBeEqualTo("REQUEST_VARS")
        task.saveResponseParameters.shouldBeEqualTo("RESPONSE_HEADERS")
        task.resultVariablePrefix.shouldBeEqualTo("RES_PREFIX")
        task.saveResponseParametersTransient.shouldBeEqualTo("TRANSIENT_RESPONSE")
        task.saveResponseVariableAsJson.shouldBeEqualTo("AS_JSON")

        val props = BpmnProcessObject(processObject.process, null,  processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.REQUEST_METHOD]!!.value.shouldBeEqualTo(task.requestMethod)
        props[PropertyType.REQUEST_URL]!!.value.shouldBeEqualTo(task.requestUrl)
        props[PropertyType.REQUEST_HEADERS]!!.value.shouldBeEqualTo(task.requestHeaders)
        props[PropertyType.REQUEST_BODY]!!.value.shouldBeEqualTo(task.requestBody)
        props[PropertyType.REQUEST_BODY_ENCODING]!!.value.shouldBeEqualTo(task.requestBodyEncoding)
        props[PropertyType.REQUEST_TIMEOUT]!!.value.shouldBeEqualTo(task.requestTimeout)
        props[PropertyType.DISALLOW_REDIRECTS]!!.value.shouldBeEqualTo(task.disallowRedirects)
        props[PropertyType.FAIL_STATUS_CODES]!!.value.shouldBeEqualTo(task.failStatusCodes)
        props[PropertyType.HANDLE_STATUS_CODES]!!.value.shouldBeEqualTo(task.handleStatusCodes)
        props[PropertyType.RESPONSE_VARIABLE_NAME]!!.value.shouldBeEqualTo(task.responseVariableName)
        props[PropertyType.IGNORE_EXCEPTION]!!.value.shouldBeEqualTo(task.ignoreException)
        props[PropertyType.SAVE_REQUEST_VARIABLES]!!.value.shouldBeEqualTo(task.saveRequestVariables)
        props[PropertyType.SAVE_RESPONSE_PARAMETERS]!!.value.shouldBeEqualTo(task.saveResponseParameters)
        props[PropertyType.RESULT_VARIABLE_PREFIX]!!.value.shouldBeEqualTo(task.resultVariablePrefix)
        props[PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT]!!.value.shouldBeEqualTo(task.saveResponseParametersTransient)
        props[PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON]!!.value.shouldBeEqualTo(task.saveResponseVariableAsJson)
    }

    @Test
    fun `Http task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);

        {value: String -> readAndUpdate(PropertyType.REQUEST_METHOD, value).requestMethod.shouldBeEqualTo(value)} ("POST");
        {value: String -> readAndUpdate(PropertyType.REQUEST_URL, value).requestUrl.shouldBeEqualTo(value)} ("http://example.com/other");
        {value: String -> readAndUpdate(PropertyType.REQUEST_HEADERS, value).requestHeaders.shouldBeEqualTo(value)} ("HDR");
        {value: String -> readAndUpdate(PropertyType.REQUEST_BODY, value).requestBody.shouldBeEqualTo(value)} ("<xml></xml>");
        {value: String -> readAndUpdate(PropertyType.REQUEST_BODY_ENCODING, value).requestBodyEncoding.shouldBeEqualTo(value)} ("ISO-8859-1");
        {value: String -> readAndUpdate(PropertyType.REQUEST_TIMEOUT, value).requestTimeout.shouldBeEqualTo(value)} ("10H");
        {value: Boolean -> readAndUpdate(PropertyType.DISALLOW_REDIRECTS, value).disallowRedirects.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.FAIL_STATUS_CODES, value).failStatusCodes.shouldBeEqualTo(value)} ("300");
        {value: String -> readAndUpdate(PropertyType.HANDLE_STATUS_CODES, value).handleStatusCodes.shouldBeEqualTo(value)} ("404,405");
        {value: String -> readAndUpdate(PropertyType.RESPONSE_VARIABLE_NAME, value).responseVariableName.shouldBeEqualTo(value)} ("RESP1");
        {value: String -> readAndUpdate(PropertyType.IGNORE_EXCEPTION, value).ignoreException.shouldBeEqualTo(value)} ("someExc1");
        {value: String -> readAndUpdate(PropertyType.SAVE_REQUEST_VARIABLES, value).saveRequestVariables.shouldBeEqualTo(value)} ("REQ_VAR1");
        {value: String -> readAndUpdate(PropertyType.SAVE_RESPONSE_PARAMETERS, value).saveResponseParameters.shouldBeEqualTo(value)} ("RESP_PAR1");
        {value: String -> readAndUpdate(PropertyType.RESULT_VARIABLE_PREFIX, value).resultVariablePrefix.shouldBeEqualTo(value)} ("RES_VAR_PREFIX");
        {value: String -> readAndUpdate(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT, value).saveResponseParametersTransient.shouldBeEqualTo(value)} ("TRANSIENT_DATA");
        {value: String -> readAndUpdate(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON, value).saveResponseVariableAsJson.shouldBeEqualTo(value)} ("AS_OTHER_JSON")
    }

    @Test
    fun `Http task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()

        readAndSetNullString(PropertyType.REQUEST_METHOD).requestMethod.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.REQUEST_URL).requestUrl.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.REQUEST_HEADERS).requestHeaders.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.REQUEST_BODY).requestBody.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.REQUEST_BODY_ENCODING).requestBodyEncoding.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.REQUEST_TIMEOUT).requestTimeout.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.FAIL_STATUS_CODES).failStatusCodes.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.HANDLE_STATUS_CODES).handleStatusCodes.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.RESPONSE_VARIABLE_NAME).responseVariableName.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.IGNORE_EXCEPTION).ignoreException.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SAVE_REQUEST_VARIABLES).saveRequestVariables.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SAVE_RESPONSE_PARAMETERS).saveResponseParameters.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.RESULT_VARIABLE_PREFIX).resultVariablePrefix.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SAVE_RESPONSE_PARAMETERS_TRANSIENT).saveResponseParametersTransient.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SAVE_RESPONSE_VARIABLE_AS_JSON).saveResponseVariableAsJson.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnHttpTask {
        return readHttpTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnHttpTask {
        return readHttpTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnHttpTask {
        return readHttpTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readHttpTask(processObject: BpmnProcessObject): BpmnHttpTask {
        return processObject.process.body!!.httpTask!!.shouldHaveSingleItem()
    }
}