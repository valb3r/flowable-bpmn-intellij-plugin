package com.valb3r.bpmn.intellij.plugin.flowable.parser.customevents

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

private const val FILE = "custom-events/start-event-with-form-properties.bpmn20.xml"

internal class FlowableUsereventWithNestedExtensionTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("startEvent1")

    @Test
    fun `Start event with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val event = readStartEventWithExtensions(processObject)
        event.id.shouldBeEqualTo(elementId)

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(FlowableObjectFactory()).processes[0].processElemPropertiesByElementId[event.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(event.id.id)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("property1", listOf("property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property("Property #1", listOf("property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("string", listOf("property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldContainSame(arrayOf(
            Property("var1", listOf("property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("val", listOf("property1"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldContainSame(arrayOf(
            Property("\${prop}", listOf("property1"))
        ))
    }

    @Test
    fun `Start event nested elements are updatable`() {
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_ID, value, "property1").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_NAME, value, "property1").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_TYPE, value, "property1").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VARIABLE, value, "property1").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DEFAULT, value, "property1").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_EXPRESSION, value, "property1").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "property1").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_ID, value, "property1,").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "property1,").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `Start event nested elements are emptyable`() {
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_ID, value, "property1").formPropertiesExtension?.shouldHaveSize(0)} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_NAME, value, "property1").formPropertiesExtension!![0].name.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_TYPE, value, "property1").formPropertiesExtension!![0].type.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VARIABLE, value, "property1").formPropertiesExtension!![0].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DEFAULT, value, "property1").formPropertiesExtension!![0].default.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_EXPRESSION, value, "property1").formPropertiesExtension!![0].expression.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "property1").formPropertiesExtension!![0].datePattern.shouldBeNull()} ("");
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnStartEvent {
        return readStartEventWithExtensions(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventWithExtensions(processObject: BpmnFileObject): BpmnStartEvent {
        return processObject.processes[0].body!!.startEvent!!.shouldHaveSize(1)[0]
    }
}
