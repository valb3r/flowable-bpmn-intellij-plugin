package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.*
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
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

        val props = BpmnProcessObject(processObject.process, null,  processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.FAILED_JOB_RETRY_CYCLE]!!.value.shouldBeEqualTo(task.failedJobRetryTimeCycle)

        props.getAll(PropertyType.FIELD_NAME)[0].value.shouldBeEqualTo("recipient")
        props.getAll(PropertyType.FIELD_EXPRESSION)[0].value.shouldBeEqualTo("userId:\${accountId}")
        props.getAll(PropertyType.FIELD_STRING)[0].value.shouldBeNull()

        props.getAll(PropertyType.FIELD_NAME)[1].value.shouldBeEqualTo("multiline")
        props.getAll(PropertyType.FIELD_EXPRESSION)[1].value.shouldBeNull()
        props.getAll(PropertyType.FIELD_STRING)[1].value.shouldBeEqualTo("This\n" +
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
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_NAME, "recipient", "activiti:field name=\"recipient\"", "<activiti:field>", "<activiti:field/>", "<activiti:expression><![CDATA[userId:\${accountId}]]")
            .fieldsExtension?.map { it.name }?.shouldNotContain("recipient")
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_EXPRESSION, "recipient", "<activiti:expression><![CDATA[userId:\${accountId}]]").fieldsExtension!![0].expression.shouldBeNullOrEmpty()
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_STRING, "multiline", "activiti:string").fieldsExtension!![1].string.shouldBeNullOrEmpty()
    }

    @Test
    fun `Add nested extension element`() {
        val process = readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = listOf("")))
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, null, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.shouldBeEqualTo(Property("new name", listOf("new name")))
    }

    @Test
    fun `Add and remove nested extension element`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = listOf("name")),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "", propertyIndex = listOf("new name"))
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, null, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]?.value.shouldBeNull()
    }

    @Test
    fun `Add and remove and add nested extension element`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = listOf("")),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "", propertyIndex = listOf("new name")),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "other new name", propertyIndex = listOf("")),
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, null, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.shouldBeEqualTo(Property("other new name", listOf("other new name")))
    }

    @Test
    fun `Add multiple nested extension elements`() {
        val process = readAndUpdateProcess(
            parser,
            FILE,
            listOf(
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_EXPRESSION, "expression 1", propertyIndex = listOf("")),
                StringValueUpdatedEvent(emptyElementId, PropertyType.FIELD_NAME, "new name", propertyIndex = listOf("")),
            )
        )
        val emptyTask = process.process.body!!.serviceTask!!.firstOrNull {it.id == emptyElementId}.shouldNotBeNull()
        val props = BpmnProcessObject(process.process, null, process.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[emptyTask.id]!!
        props[PropertyType.FIELD_NAME]!!.shouldBeEqualTo(Property("new name", listOf("new name")))
        props[PropertyType.FIELD_EXPRESSION]!!.shouldBeEqualTo(Property("expression 1", listOf("new name")))
    }

    private fun readAndSetNullStringAndAssertItIsRemoved(property: PropertyType, propertyIndex: String, vararg shouldNotContainStr: String): BpmnServiceTask {
        val event = StringValueUpdatedEvent(elementId, property, "", propertyIndex = listOf(propertyIndex))
        val updated = updateBpmnFile(parser, FILE, listOf(event))
        shouldNotContainStr.forEach { updated.shouldNotContain(it) }
        return readServiceTaskWithExtensions(parser.parse(updated))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnServiceTask {
        return readServiceTaskWithExtensions(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = listOf(propertyIndex))))
    }

    private fun readServiceTaskWithExtensions(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSize(3)[0]
    }
}
