package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivityObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnMailTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val FILE = "custom-service-tasks/mail-task.bpmn20.xml"

internal class ActivityMailTaskTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("mailTaskId")

    @Test
    fun `Mail task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readMailTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Mail task name")
        task.documentation.shouldBeEqualTo("Docs for mail task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.headers.shouldBeNull() // Activity does not support headers
        task.to.shouldBeEqualTo("bar@example.com")
        task.from.shouldBeEqualTo("foo@example.com")
        task.subject.shouldBeEqualTo("Got to be drunk")
        task.cc.shouldBeEqualTo("foo-cc@example.com")
        task.bcc.shouldBeEqualTo("foo-bcc@example.com")
        task.text.shouldBeEqualTo("Hello Mr. Bar!")
        task.html.shouldBeEqualTo("<html>Hello</html>")
        task.charset.shouldBeEqualTo("UTF-8")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivityObjectFactory()).elemPropertiesByElementId[task.id]!!
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
        {value: String -> assertThrows<IllegalStateException> {readAndUpdate(PropertyType.HEADERS, value).headers.shouldBeEqualTo(value)}} ("Header111");
        {value: String -> readAndUpdate(PropertyType.TO, value).to.shouldBeEqualTo(value)} ("to@bar.example.com");
        {value: String -> readAndUpdate(PropertyType.FROM, value).from.shouldBeEqualTo(value)} ("from@bar.example.com");
        {value: String -> readAndUpdate(PropertyType.SUBJECT, value).subject.shouldBeEqualTo(value)} ("Some subject to discuss?");
        {value: String -> readAndUpdate(PropertyType.CC, value).cc.shouldBeEqualTo(value)} ("john@example.com");
        {value: String -> readAndUpdate(PropertyType.BCC, value).bcc.shouldBeEqualTo(value)} ("jane@example.com");
        {value: String -> readAndUpdate(PropertyType.TEXT, value).text.shouldBeEqualTo(value)} ("A message?");
        {value: String -> readAndUpdate(PropertyType.HTML, value).html.shouldBeEqualTo(value)} ("<html></html>");
        {value: String -> readAndUpdate(PropertyType.CHARSET, value).charset.shouldBeEqualTo(value)} ("ISO-8859-1")
    }

    @Test
    fun `Mail task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        assertThrows<IllegalStateException> {readAndSetNullString(PropertyType.HEADERS)}
        readAndSetNullString(PropertyType.TO).to.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.FROM).from.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SUBJECT).subject.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CC).cc.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.BCC).bcc.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TEXT).text.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.HTML).html.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CHARSET).charset.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnMailTask {
        return readMailTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnMailTask {
        return readMailTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnMailTask {
        return readMailTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readMailTask(processObject: BpmnProcessObject): BpmnMailTask {
        return processObject.process.body!!.mailTask!!.shouldHaveSingleItem()
    }
}