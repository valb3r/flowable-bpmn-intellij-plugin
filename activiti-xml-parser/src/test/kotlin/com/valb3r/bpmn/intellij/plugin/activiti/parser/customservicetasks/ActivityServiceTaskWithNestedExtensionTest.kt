package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.*
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.ValueInArray
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task-with-nested-extensions.bpmn20.xml"

internal class ActivityServiceTaskWithNestedExtensionTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("serviceTaskWithExtensionId")
    private val emptyElementId = BpmnElementId("emptyServiceTaskId")

    @Test
    fun `Service task with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readServiceTaskWithExtensions(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Service task with extension")
        task.documentation.shouldBeNull()
        task.failedJobRetryTimeCycle?.shouldBeEqualTo("R10/PT5M")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.FAILED_JOB_RETRY_CYCLE]!!.value.shouldBeEqualTo(task.failedJobRetryTimeCycle)

        props.getAll(PropertyType.FIELD_NAME)[0].grpVal().value.shouldBeEqualTo("recipient")
        props.getAll(PropertyType.FIELD_EXPRESSION)[0].grpVal().value.shouldBeEqualTo("userId:\${accountId}")
        props.getAll(PropertyType.FIELD_STRING)[0].grpVal().value.shouldBeNull()

        props.getAll(PropertyType.FIELD_NAME)[1].grpVal().value.shouldBeEqualTo("multiline")
        props.getAll(PropertyType.FIELD_EXPRESSION)[1].grpVal().value.shouldBeNull()
        props.getAll(PropertyType.FIELD_STRING)[1].grpVal().value.shouldBeEqualTo("This\n" +
                "                is\n" +
                "                multiline\n" +
                "                text\n" +
                "                ")
    }

    @Test
    fun `Service task nested elements are updatable`() {
        {value: String -> readAndUpdate(PropertyType.FIELD_NAME, value, "recipient").fieldsExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdate(PropertyType.FIELD_EXPRESSION, value, "recipient").fieldsExtension!![0].expression.shouldBeEqualTo(value)} ("EXPR0=EXPR1");
        {value: String -> readAndUpdate(PropertyType.FIELD_NAME, value, "multiline").fieldsExtension!![1].name.shouldBeEqualTo(value)} ("new mulitline name");
        {value: String -> readAndUpdate(PropertyType.FIELD_STRING, value, "multiline").fieldsExtension!![1].string.shouldBeEqualTo(value)} ("  new \n   multiline  ")
    }

    @Test
    fun `Service task nested elements are emptyable`() {
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_NAME, "recipient", "activiti:field name=\"recipient\"", "<activiti:field>", "<activiti:field/>", "activiti:expression")
            .fieldsExtension?.map { it.name }?.shouldNotContain("recipient")
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_EXPRESSION, "recipient", "activiti:expression").fieldsExtension!![0].expression.shouldBeNullOrEmpty()
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_STRING, "multiline", "activiti:string").fieldsExtension!![1].string.shouldBeNullOrEmpty()
    }

    @Test
    fun `Add nested extension element`() {
        val process = readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = ""))
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.value.shouldBeEqualTo(ValueInArray("new name", "new name"))
    }

    @Test
    fun `Add and remove nested extension element`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = "name"),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "", propertyIndex = "new name")
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.value.shouldBeNull()
    }

    @Test
    fun `Add and remove and add nested extension element`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = ""),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "", propertyIndex = "new name"),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "other new name", propertyIndex = ""),
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.value.shouldBeEqualTo(ValueInArray("other new name", "other new name"))
    }

    @Test
    fun `Add multiple nested extension elements`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_EXPRESSION, "expression 1", propertyIndex = ""),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = ""),
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.value.shouldBeEqualTo(ValueInArray("new name", "new name"))
        props[PropertyType.FIELD_EXPRESSION]!!.value.shouldBeEqualTo(ValueInArray("new name", "expression 1"))
    }

    private fun readAndSetNullStringAndAssertItIsRemoved(property: PropertyType, propertyIndex: String, vararg shouldNotContainStr: String): BpmnServiceTask {
        val event = StringValueUpdatedEvent(elementId, property, "", propertyIndex = propertyIndex)
        val updated = updateBpmnFile(parser, FILE, listOf(event))
        shouldNotContainStr.forEach { updated.shouldNotContain(it) }
        return readServiceTaskWithExtensions(parser.parse(updated))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnServiceTask {
        return readServiceTaskWithExtensions(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = propertyIndex)))
    }

    private fun readServiceTaskWithExtensions(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSize(2)[0]
    }

    private fun Property.grpVal(): ValueInArray {
        return this.value as ValueInArray
    }
}
