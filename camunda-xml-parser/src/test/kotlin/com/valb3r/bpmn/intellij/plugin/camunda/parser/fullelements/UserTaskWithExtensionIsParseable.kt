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
    private val multiplePropElementId = BpmnElementId("detailedUserTaskMultiAll")

    @Test
    fun `Start event (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("User task with single extension")
        task.documentation.shouldBeEqualTo("Some docs")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("formFieldId", listOf("formFieldId"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property("someFormField", listOf("formFieldId"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("long", listOf("formFieldId"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("1", listOf("formFieldId"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DATE_PATTERN).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_VALUE_ID).shouldContainSame(arrayOf(
            Property("fieldProperty", listOf("formFieldId", "fieldProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_NAME).shouldContainSame(arrayOf(
            Property("propertyValue", listOf("formFieldId", "fieldProperty"))
        ))
    }

    @Test
    fun `Start event (single props) is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    @Test
    fun `Start event (single props)  nested elements are updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_ID, value, "formFieldId").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_NAME, value, "formFieldId").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formFieldId").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formFieldId").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formFieldId").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formFieldId").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formFieldId").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formFieldId,fieldProperty").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formFieldId,fieldProperty").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `Start event (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_ID, value, "formFieldId").formPropertiesExtension?.shouldHaveSize(0)} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_NAME, value, "formFieldId").formPropertiesExtension!![0].name.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formFieldId").formPropertiesExtension!![0].type.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formFieldId").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formFieldId").formPropertiesExtension!![0].default.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formFieldId").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formFieldId").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formFieldId,fieldProperty").formPropertiesExtension!![0].value.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formFieldId,fieldProperty").formPropertiesExtension!![0].value!![0].name.shouldBeNull()} ("");
    }

    @Test
    fun `Start event (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiProp(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("Start event(multi)")
        task.documentation.shouldBeEqualTo("As full as possible start event\nmultiline")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("formFieldId1", listOf("formFieldId1")), Property("formFieldId2", listOf("formFieldId2"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property("someFormField", listOf("formFieldId1")), Property("someLabel", listOf("formFieldId2"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("long", listOf("formFieldId1")), Property("date", listOf("formFieldId2"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("1", listOf("formFieldId1")), Property("2020-01-01", listOf("formFieldId2"))
        ))

        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_DATE_PATTERN).shouldBeEmpty() // Camunda does not seem to support this field

        props.getAll(PropertyType.FORM_PROPERTY_VALUE_ID).shouldContainSame(arrayOf(
            Property("fieldProperty1", listOf("formFieldId1", "fieldProperty1")),
            Property("fieldProperty2", listOf("formFieldId1", "fieldProperty2")),
            Property("formFieldProperty1", listOf("formFieldId2", "formFieldProperty1")),
            Property("formFieldProperty2", listOf("formFieldId2", "formFieldProperty2")),
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_NAME).shouldContainSame(arrayOf(
            Property("propertyValue", listOf("formFieldId1", "fieldProperty1")),
            Property("propertyValue2", listOf("formFieldId1", "fieldProperty2")),
            Property("123", listOf("formFieldId2", "formFieldProperty1")),
            Property("fooBar", listOf("formFieldId2", "formFieldProperty2"))
        ))
    }

    @Test
    fun `Start event (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    @Test
    fun `Start event (multiple props)  nested elements are updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_ID, value, "formFieldId1").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_NAME, value, "formFieldId1").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formFieldId1").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formFieldId").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formFieldId1").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formFieldId").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formFieldId").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formFieldId1,fieldProperty1").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formFieldId1,fieldProperty1").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `Start event (multiple props) nested elements are emptyable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_ID, value, "formFieldId1").formPropertiesExtension?.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_NAME, value, "formFieldId1").formPropertiesExtension!![0].name.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_TYPE, value, "formFieldId1").formPropertiesExtension!![0].type.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "formFieldId").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "formFieldId1").formPropertiesExtension!![0].default.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "formFieldId").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        // Unsupported {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "formFieldId").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formFieldId1,fieldProperty1").formPropertiesExtension!![0].value?.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formFieldId1,fieldProperty1").formPropertiesExtension!![0].value!![0].name.shouldBeNull()} ("");
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