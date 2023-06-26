package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "fullelements/full-user-task.bpmn"

internal class UserTaskWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("detailedUserTaskSingleAll")
    private val multiplePropElementId = BpmnElementId("detailedUserTaskMultipleAll")

    @Test
    fun `User task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("User task with single extension")
        task.assignee.shouldBeEqualTo("someAssignee")
        task.candidateUsers.shouldBeEqualTo("candidateUser1")
        task.candidateGroups.shouldBeEqualTo("candidates1")
        task.documentation.shouldBeEqualTo("Some docs")

        val props = BpmnProcessObject(processObject.process, null,  processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.ASSIGNEE]!!.value.shouldBeEqualTo(task.assignee)
        props[PropertyType.CANDIDATE_USERS]!!.value.shouldBeEqualTo(task.candidateUsers)
        props[PropertyType.CANDIDATE_GROUPS]!!.value.shouldBeEqualTo(task.candidateGroups)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("formField", listOf("formField"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property("formFieldValue", listOf("formField"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("string", listOf("formField"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("123", listOf("formField"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DATE_PATTERN).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_VALUE_ID).shouldContainSame(arrayOf(
            Property("property1", listOf("formField", "property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_NAME).shouldContainSame(arrayOf(
            Property("value1", listOf("formField", "property1"))
        ))
    }

    @Test
    fun `User task (single props) is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.ASSIGNEE, value).assignee.shouldBeEqualTo(value)} ("new Assignee");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.CANDIDATE_USERS, value).candidateUsers.shouldBeEqualTo(value)} ("new Candidate");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.CANDIDATE_GROUPS, value).candidateGroups.shouldBeEqualTo(value)} ("new Group");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    @Test
    fun `User task (single props)  nested elements are updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_ID, value, "formField").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_NAME, value, "formField").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formField").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formField").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formField").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formField").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formField").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formField,property1").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formField,property1").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `User task (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_ID, value, "formField").formPropertiesExtension?.shouldHaveSize(0)} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_NAME, value, "formField").formPropertiesExtension!![0].name.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formField").formPropertiesExtension!![0].type.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formField").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formField").formPropertiesExtension!![0].default.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formField").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formField").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formField,property1").formPropertiesExtension!![0].value.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formField,property1").formPropertiesExtension!![0].value!![0].name.shouldBeNull()} ("");
    }

    @Test
    fun `User task (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiProp(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("User task with multiple extension")
        task.assignee.shouldBeEqualTo("someAssignee")
        task.candidateUsers.shouldBeEqualTo("candidateUser1")
        task.candidateGroups.shouldBeEqualTo("candidates1")
        task.documentation.shouldBeEqualTo("Some docs")

        val props = BpmnProcessObject(processObject.process, null,  processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.ASSIGNEE]!!.value.shouldBeEqualTo(task.assignee)
        props[PropertyType.CANDIDATE_USERS]!!.value.shouldBeEqualTo(task.candidateUsers)
        props[PropertyType.CANDIDATE_GROUPS]!!.value.shouldBeEqualTo(task.candidateGroups)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("formField1", listOf("formField1")), Property("formField2", listOf("formField2"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property("formFieldValue1", listOf("formField1")), Property("label2", listOf("formField2"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("string", listOf("formField1")), Property("long", listOf("formField2"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("123", listOf("formField1")), Property("12", listOf("formField2"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DATE_PATTERN).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_VALUE_ID).shouldContainSame(arrayOf(
            Property("property1", listOf("formField1", "property1")),
            Property("property2", listOf("formField1", "property2")),
            Property("prop1", listOf("formField2", "prop1")),
            Property("prop2", listOf("formField2", "prop2")),
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_NAME).shouldContainSame(arrayOf(
            Property("value1", listOf("formField1", "property1")),
            Property("value2", listOf("formField1", "property2")),
            Property("value1", listOf("formField2", "prop1")),
            Property("value2", listOf("formField2", "prop2"))
        ))
    }

    @Test
    fun `User task (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.ASSIGNEE, value).assignee.shouldBeEqualTo(value)} ("new Assignee");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.CANDIDATE_USERS, value).candidateUsers.shouldBeEqualTo(value)} ("new Candidate");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.CANDIDATE_GROUPS, value).candidateGroups.shouldBeEqualTo(value)} ("new Group");
    }

    @Test
    fun `User task (multiple props)  nested elements are updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_ID, value, "formField1").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_NAME, value, "formField1").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formField1").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formField").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formField1").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formField").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formField").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formField1,property1").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formField1,property1").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `User task (multiple props) nested elements are emptyable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_ID, value, "formField1").formPropertiesExtension?.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_NAME, value, "formField1").formPropertiesExtension!![0].name.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formField1").formPropertiesExtension!![0].type.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formField").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formField1").formPropertiesExtension!![0].default.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formField").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formField").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formField1,property1").formPropertiesExtension!![0].value?.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formField1,property1").formPropertiesExtension!![0].value!![0].name.shouldBeNull()} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnUserTask {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnUserTask {
        return readStartEventMultiProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiplePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiProp(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSize(2)[1]
    }
}