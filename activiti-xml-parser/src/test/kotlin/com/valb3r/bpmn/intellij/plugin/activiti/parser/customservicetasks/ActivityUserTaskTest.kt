package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val FILE = "custom-service-tasks/user-task.bpmn20.xml"

internal class ActivityUserTaskTest {

    private val parser = ActivitiParser()
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
        task.candidateUsers.shouldBeEqualTo("user1")
        task.candidateGroups.shouldBeEqualTo("group1")
        task.dueDate.shouldBeEqualTo("2020-01-01")
        task.category.shouldBeNull() // Unsupported by Activity
        task.formKey.shouldBeEqualTo("FORM_KEY")
        task.formFieldValidation.shouldBeNull() // Unsupported by Activity
        task.priority.shouldBeEqualTo("1")
        task.skipExpression.shouldBeNull() // Unsupported by Activity

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.ASSIGNEE]!!.value.shouldBeEqualTo(task.assignee)
        props[PropertyType.CANDIDATE_USERS]!!.value.shouldBeEqualTo(task.candidateUsers)
        props[PropertyType.CANDIDATE_GROUPS]!!.value.shouldBeEqualTo(task.candidateGroups)
        props[PropertyType.DUE_DATE]!!.value.shouldBeEqualTo(task.dueDate)
        props[PropertyType.CATEGORY].shouldBeNull()
        props[PropertyType.FORM_KEY]!!.value.shouldBeEqualTo(task.formKey)
        props[PropertyType.FORM_FIELD_VALIDATION].shouldBeNull()
        props[PropertyType.PRIORITY]!!.value.shouldBeEqualTo(task.priority)
        props[PropertyType.SKIP_EXPRESSION].shouldBeNull()
    }

    @Test
    fun `User task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.ASSIGNEE, value).assignee.shouldBeEqualTo(value)} ("Assigned to");
        {value: String -> readAndUpdate(PropertyType.CANDIDATE_USERS, value).candidateUsers.shouldBeEqualTo(value)} ("User");
        {value: String -> readAndUpdate(PropertyType.CANDIDATE_GROUPS, value).candidateGroups.shouldBeEqualTo(value)} ("Group");
        {value: String -> readAndUpdate(PropertyType.DUE_DATE, value).dueDate.shouldBeEqualTo(value)} ("2000-01-01");
        {value: String -> readAndUpdate(PropertyType.CATEGORY, value).category.shouldBeNull()} ("SOME_CAT123");
        {value: String -> readAndUpdate(PropertyType.FORM_KEY, value).formKey.shouldBeEqualTo(value)} ("KEY_90");
        {value: Boolean -> readAndUpdate(PropertyType.FORM_FIELD_VALIDATION, value).formFieldValidation.shouldBeNull()} (false);
        {value: String -> readAndUpdate(PropertyType.PRIORITY, value).priority.shouldBeEqualTo(value)} ("22");
        {value: String -> readAndUpdate(PropertyType.SKIP_EXPRESSION, value).skipExpression.shouldBeNull()} ("#{something.wrong()}")
    }

    @Test
    fun `User task is fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.ASSIGNEE).assignee.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DUE_DATE).dueDate.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CANDIDATE_USERS).candidateUsers.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CANDIDATE_GROUPS).candidateGroups.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.CATEGORY).category.shouldBeNull()
        readAndSetNullString(PropertyType.FORM_KEY).formKey.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.PRIORITY).priority.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.SKIP_EXPRESSION).skipExpression.shouldBeNull()
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

    private fun readUserTask(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSingleItem()
    }
}