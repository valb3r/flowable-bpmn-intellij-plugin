package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "fullelements/full-service-task.bpmn"

internal class ServiceTaskWithExtensionIsParseable {

    private val parser = CamundaParser()
    private val singlePropElementId = BpmnElementId("detailedServiceTaskSingleAll")
    private val multiplePropElementId = BpmnElementId("detailedServiceTaskMultipleAll")

    @Test
    fun `Service task (single props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventSingleProp(processObject)
        task.id.shouldBeEqualTo(singlePropElementId)
        task.name.shouldBeEqualTo("Service task with single extension")
        task.documentation.shouldBeEqualTo("Some docs")

        val props = BpmnFileObject(processObject.processes, processObject.collaborations, processObject.diagram).toView(CamundaObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FIELD_NAME).shouldContainSame(arrayOf(
            Property("field", listOf("field"))
        ))
        props.getAll(PropertyType.FIELD_STRING).shouldContainSame(arrayOf(
            Property("fieldValue", listOf("field"))
        ))
        props.getAll(PropertyType.FIELD_EXPRESSION).shouldContainSame(arrayOf(
            Property( null, listOf("field"))
        ))
    }

    @Test
    fun `Service task (single props) is updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    @Test
    fun `Service task (single props)  nested elements are updatable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_NAME, value, "field").fieldsExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_STRING, value, "field").fieldsExtension!![0].string.shouldBeEqualTo(value)} ("new string");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_EXPRESSION, value, "field").fieldsExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
    }

    @Test
    fun `Service task (single props) nested elements are emptyable`() {
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_NAME, value, "field").fieldsExtension!!.shouldBeEmpty()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_STRING, value, "field").fieldsExtension!![0].string.shouldBeNull()} ("");
        {value: String -> readAndUpdateSinglePropTask(PropertyType.FIELD_EXPRESSION, value, "field").fieldsExtension!![0].expression.shouldBeNull()} ("");
    }

    @Test
    fun `Service task (multiple props) with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readStartEventMultiProp(processObject)
        task.id.shouldBeEqualTo(multiplePropElementId)
        task.name.shouldBeEqualTo("Service task with multiple extensions")
        task.documentation.shouldBeEqualTo("Some docs")

        val props = BpmnFileObject(processObject.processes, processObject.collaborations, processObject.diagram).toView(CamundaObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)

        props.getAll(PropertyType.FIELD_NAME).shouldContainSame(arrayOf(
            Property("field1", listOf("field1")), Property("field2", listOf("field2"))
        ))
        props.getAll(PropertyType.FIELD_STRING).shouldContainSame(arrayOf(
            Property("fieldValue", listOf("field1")), Property(null, listOf("field2"))
        ))
        props.getAll(PropertyType.FIELD_EXPRESSION).shouldContainSame(arrayOf(
            Property( null, listOf("field1")), Property( "\${someExpr}", listOf("field2"))
        ))
    }

    @Test
    fun `Service task (multiple props) is updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
    }

    @Test
    fun `Service task (multiple props)  nested elements are updatable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_NAME, value, "field1").fieldsExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_STRING, value, "field1").fieldsExtension!![0].string.shouldBeEqualTo(value)} ("new string");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_EXPRESSION, value, "field1").fieldsExtension!![0].expression.shouldBeEqualTo(value)} ("new expression");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_NAME, value, "field2").fieldsExtension!![1].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_STRING, value, "field2").fieldsExtension!![1].string.shouldBeEqualTo(value)} ("new string");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_EXPRESSION, value, "field2").fieldsExtension!![1].expression.shouldBeEqualTo(value)} ("new expression");
    }

    @Test
    fun `Service task (multiple props) nested elements are emptyable`() {
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_NAME, value, "field1").fieldsExtension!!.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_STRING, value, "field1").fieldsExtension!![0].string.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_EXPRESSION, value, "field1").fieldsExtension!![0].expression.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_NAME, value, "field2").fieldsExtension!!.shouldHaveSize(1)} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_STRING, value, "field2").fieldsExtension!![1].string.shouldBeNull()} ("");
        {value: String -> readAndUpdateMultiPropTask(PropertyType.FIELD_EXPRESSION, value, "field2").fieldsExtension!![1].expression.shouldBeNull()} ("");
    }

    private fun readAndUpdateSinglePropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventSingleProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(singlePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventSingleProp(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.processes[0].body!!.serviceTask!!.shouldHaveSize(2)[0]
    }

    private fun readAndUpdateMultiPropTask(property: PropertyType, newValue: String, propertyIndex: String = ""): BpmnServiceTask {
        return readStartEventMultiProp(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(multiplePropElementId, property, newValue, propertyIndex = propertyIndex.split(","))))
    }

    private fun readStartEventMultiProp(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.processes[0].body!!.serviceTask!!.shouldHaveSize(2)[1]
    }
}
