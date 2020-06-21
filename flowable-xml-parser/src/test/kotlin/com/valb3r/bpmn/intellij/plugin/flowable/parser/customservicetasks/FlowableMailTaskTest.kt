package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnMailTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/mail-task.bpmn20.xml"

internal class FlowableMailTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("mailTaskId")

    @Test
    fun `Mail task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readCamelTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Mail task name")
        task.documentation.shouldBeEqualTo("Docs for mail task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.headers.shouldBeEqualTo("Header1,Header2")
        task.to.shouldBeEqualTo("bar@example.com")
        task.from.shouldBeEqualTo("foo@example.com")
        task.subject.shouldBeEqualTo("Got to be drunk")
        task.cc.shouldBeEqualTo("foo-cc@example.com")
        task.bcc.shouldBeEqualTo("foo-bcc@example.com")
        task.text.shouldBeEqualTo("Hello Mr. Bar!")
        task.html.shouldBeEqualTo("<html>Hello</html>")
        task.charset.shouldBeEqualTo("UTF-8")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.HEADERS]!!.value.shouldBeEqualTo(task.headers)
        props[PropertyType.TO]!!.value.shouldBeEqualTo(task.to)
        props[PropertyType.FROM]!!.value.shouldBeEqualTo(task.from)
        props[PropertyType.SUBJECT]!!.value.shouldBeEqualTo(task.subject)
        props[PropertyType.CC]!!.value.shouldBeEqualTo(task.cc)
        props[PropertyType.BCC]!!.value.shouldBeEqualTo(task.bcc)
        props[PropertyType.TEXT]!!.value.shouldBeEqualTo(task.text)
        props[PropertyType.HTML]!!.value.shouldBeEqualTo(task.html)
        props[PropertyType.CHARSET]!!.value.shouldBeEqualTo(task.charset)
    }

    @Test
    fun `Mail task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.HEADERS, value).headers.shouldBeEqualTo(value)} ("Header111");
        {value: String -> readAndUpdate(PropertyType.TO, value).to.shouldBeEqualTo(value)} ("to@bar.example.com");
        {value: String -> readAndUpdate(PropertyType.FROM, value).from.shouldBeEqualTo(value)} ("from@bar.example.com");
        {value: String -> readAndUpdate(PropertyType.SUBJECT, value).subject.shouldBeEqualTo(value)} ("Some subject to discuss?");
        {value: String -> readAndUpdate(PropertyType.CC, value).cc.shouldBeEqualTo(value)} ("john@example.com");
        {value: String -> readAndUpdate(PropertyType.BCC, value).bcc.shouldBeEqualTo(value)} ("jane@example.com");
        {value: String -> readAndUpdate(PropertyType.TEXT, value).text.shouldBeEqualTo(value)} ("A message?");
        {value: String -> readAndUpdate(PropertyType.HTML, value).html.shouldBeEqualTo(value)} ("<html></html>");
        {value: String -> readAndUpdate(PropertyType.CHARSET, value).charset.shouldBeEqualTo(value)} ("ISO-8859-1");
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnMailTask {
        return readCamelTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnMailTask {
        return readCamelTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readCamelTask(processObject: BpmnProcessObject): BpmnMailTask {
        val task = processObject.process.body!!.mailTask!!.shouldHaveSingleItem()
        return task
    }
}