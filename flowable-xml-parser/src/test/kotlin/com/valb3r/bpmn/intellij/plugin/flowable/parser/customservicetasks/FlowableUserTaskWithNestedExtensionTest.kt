package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
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

private const val FILE = "custom-service-tasks/user-task-with-nested-extensions.bpmn20.xml"

internal class FlowableUserTaskWithNestedExtensionTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("userTaskId")

    @Test
    fun `User task with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readUserTaskWithExtensions(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("A user task")
        task.documentation.shouldBeEqualTo("A user task to do")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldContainSame(arrayOf(
            Property("fooVariableName", listOf("fooVariableName")), Property("isSkippable", listOf("isSkippable")), Property("fullProperty", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_NAME).shouldContainSame(arrayOf(
            Property(null, listOf("fooVariableName")), Property(null, listOf("isSkippable")), Property("Full Property", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_TYPE).shouldContainSame(arrayOf(
            Property("string", listOf("fooVariableName")), Property("boolean", listOf("isSkippable")), Property("string", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VARIABLE).shouldContainSame(arrayOf(
            Property("fooDefault", listOf("fooVariableName")), Property(null, listOf("isSkippable")), Property("fullProperty", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_DEFAULT).shouldContainSame(arrayOf(
            Property("isFoo", listOf("fooVariableName")), Property(null, listOf("isSkippable")), Property("aValue", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_EXPRESSION).shouldContainSame(arrayOf(
            Property(null, listOf("fooVariableName")), Property("\${userCanSkip}", listOf("isSkippable")), Property("\${foo}", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_DATE_PATTERN).shouldContainSame(arrayOf(
            Property(null, listOf("fooVariableName")), Property(null, listOf("isSkippable")), Property("DD.MM.YYYY", listOf("fullProperty"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_ID).shouldContainSame(arrayOf(
            Property(null, listOf("fooVariableName", "")), Property(null, listOf("isSkippable", "")), Property("value1", listOf("fullProperty", "value1")), Property("value2", listOf("fullProperty", "value2"))
        ))
        props.getAll(PropertyType.FORM_PROPERTY_VALUE_NAME).shouldContainSame(arrayOf(
            Property(null, listOf("fooVariableName", "")), Property(null, listOf("isSkippable", "")), Property("Foo", listOf("fullProperty", "value1")), Property("Bar", listOf("fullProperty", "value2"))
        ))
    }

    @Test
    fun `User task nested elements are updatable`() {
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_ID, value, "fooVariableName").formPropertiesExtension!![0].id.shouldBeEqualTo(value)} ("new id");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_NAME, value, "fooVariableName").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_TYPE, value, "fooVariableName").formPropertiesExtension!![0].type.shouldBeEqualTo(value)} ("new type");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VARIABLE, value, "fooVariableName").formPropertiesExtension!![0].variable.shouldBeEqualTo(value)} ("new variable");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DEFAULT, value, "fooVariableName").formPropertiesExtension!![0].default.shouldBeEqualTo(value)} ("new default");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_EXPRESSION, value, "fooVariableName").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "fooVariableName").formPropertiesExtension!![0].datePattern.shouldBeEqualTo(value)} ("new datePattern");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_ID, value, "fooVariableName,").formPropertiesExtension!![0].value!![0].id?.shouldBeEqualTo(value)} ("new inner id");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "fooVariableName,").formPropertiesExtension!![0].value!![0].name.shouldBeEqualTo(value)} ("new inner name");
    }

    @Test
    fun `User task nested elements are emptyable`() {
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_ID, value, "fullProperty").formPropertiesExtension?.shouldHaveSize(2)} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_NAME, value, "fullProperty").formPropertiesExtension!![2].name.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_TYPE, value, "fullProperty").formPropertiesExtension!![2].type.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VARIABLE, value, "fullProperty").formPropertiesExtension!![2].variable.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DEFAULT, value, "fullProperty").formPropertiesExtension!![2].default.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_EXPRESSION, value, "fullProperty").formPropertiesExtension!![2].expression.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_DATE_PATTERN, value, "fullProperty").formPropertiesExtension!![2].datePattern.shouldBeNull()} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_ID, value, "fullProperty,value1").formPropertiesExtension!![2].value!![0].id?.shouldBeEqualTo("value2")} ("");
        {value: String -> readAndUpdate(PropertyType.FORM_PROPERTY_VALUE_NAME, value, "fullProperty,value1").formPropertiesExtension!![2].value!![0].name.shouldBeNull()} ("");
    }

    @Test
    fun `Empty user task contains extension`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readEmptyUserTaskWithExtensions(processObject)
        task.id.shouldBeEqualTo(BpmnElementId("emptyUserTaskId"))
        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props.getAll(PropertyType.FORM_PROPERTY_ID).shouldHaveSize(1)
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnUserTask {
        return readUserTaskWithExtensions(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readUserTaskWithExtensions(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSize(3)[0]
    }

    private fun readEmptyUserTaskWithExtensions(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSize(3)[2]
    }
}
