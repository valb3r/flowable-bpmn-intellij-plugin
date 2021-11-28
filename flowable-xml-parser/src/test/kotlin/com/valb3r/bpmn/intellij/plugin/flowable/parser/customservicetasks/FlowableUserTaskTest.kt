package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
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

private const val FILE = "custom-service-tasks/user-task.bpmn20.xml"

internal class FlowableUserTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("userTaskId")

    @Test
    fun `User task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readUserTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("A user task")
        task.documentation.shouldBeEqualTo("A user task to do")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.assignee.shouldBeEqualTo("\$INITIATOR")
        task.dueDate.shouldBeEqualTo("2020-01-01")
        task.category.shouldBeEqualTo("SOME_CATEGORY")
        task.formKey.shouldBeEqualTo("FORM_KEY")
        task.formFieldValidation.shouldBeEqualTo(true)
        task.priority.shouldBeEqualTo("1")
        task.skipExpression.shouldBeEqualTo("#{do.skip}")

        val props = BpmnFileObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.ASSIGNEE]!!.value.shouldBeEqualTo(task.assignee)
        props[PropertyType.DUE_DATE]!!.value.shouldBeEqualTo(task.dueDate)
        props[PropertyType.CATEGORY]!!.value.shouldBeEqualTo(task.category)
        props[PropertyType.FORM_KEY]!!.value.shouldBeEqualTo(task.formKey)
        props[PropertyType.FORM_FIELD_VALIDATION]!!.value.shouldBeEqualTo(task.formFieldValidation)
        props[PropertyType.PRIORITY]!!.value.shouldBeEqualTo(task.priority)
        props[PropertyType.SKIP_EXPRESSION]!!.value.shouldBeEqualTo(task.skipExpression)
    }

    @Test
    fun `User task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.ASSIGNEE, value).assignee.shouldBeEqualTo(value)} ("Assigned to");
        {value: String -> readAndUpdate(PropertyType.DUE_DATE, value).dueDate.shouldBeEqualTo(value)} ("2000-01-01");
        {value: String -> readAndUpdate(PropertyType.CATEGORY, value).category.shouldBeEqualTo(value)} ("SOME_CAT123");
        {value: String -> readAndUpdate(PropertyType.FORM_KEY, value).formKey.shouldBeEqualTo(value)} ("KEY_90");
        {value: Boolean -> readAndUpdate(PropertyType.FORM_FIELD_VALIDATION, value).formFieldValidation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.PRIORITY, value).priority.shouldBeEqualTo(value)} ("22");
        {value: String -> readAndUpdate(PropertyType.SKIP_EXPRESSION, value).skipExpression.shouldBeEqualTo(value)} ("#{something.wrong()}")
    }

    @Test
    fun `User task is fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ASSIGNEE).assignee.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DUE_DATE).dueDate.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CATEGORY).category.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.FORM_KEY).formKey.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.PRIORITY).priority.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SKIP_EXPRESSION).skipExpression.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnUserTask {
        return readUserTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }
    
    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnUserTask {
        return readUserTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnUserTask {
        return readUserTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readUserTask(processObject: BpmnFileObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSingleItem()
    }
}
