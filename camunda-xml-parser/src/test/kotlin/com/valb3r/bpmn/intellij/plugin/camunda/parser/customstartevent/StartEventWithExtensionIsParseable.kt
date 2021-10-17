package com.valb3r.bpmn.intellij.plugin.camunda.parser.customstartevent

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val FILE = "customevents/full-start-event.bpmn"

internal class StartEventWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("detailedStartEventSingleAll")

    @Test
    fun `Start event (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("Start event (single)")
        task.documentation.shouldBeEqualTo("As full as possible start event\nmultiline")

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
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "formFieldId,").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "formFieldId,").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `Start event (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_ID, value, "fullProperty").formPropertiesExtension?.shouldHaveSize(2)} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_NAME, value, "fullProperty").formPropertiesExtension!![2].name.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_TYPE, value, "fullProperty").formPropertiesExtension!![2].type.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VARIABLE, value, "fullProperty").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DEFAULT, value, "fullProperty").formPropertiesExtension!![2].default.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_EXPRESSION, value, "fullProperty").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "fullProperty").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_ID, value, "fullProperty,value1").formPropertiesExtension!![2].value!![0].id?.shouldBeEqualTo("value2")} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "fullProperty,value1").formPropertiesExtension!![2].value!![0].name.shouldBeNull()} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnStartEvent {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnProcessObject): BpmnStartEvent {
        return processObject.process.body!!.startEvent!!.shouldHaveSize(2)[0]
    }
}