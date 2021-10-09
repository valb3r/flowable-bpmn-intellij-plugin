package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.*
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/user-task-with-nested-extensions.bpmn20.xml"

@Disabled
internal class ActivityUserTaskWithNestedExtensionTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("userTaskId")

    @Test
    fun `User task with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readUserTaskWithExtensions(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("A user task")
        task.documentation.shouldBeEqualTo("A user task to do")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
    }

    @Test
    fun `User task nested elements are updatable`() {
        {value: String -> readAndUpdate(PropertyType.FIELD_NAME, value, "recipient").formPropertiesExtension!![0].name.shouldBeEqualTo(value)} ("new name");
        {value: String -> readAndUpdate(PropertyType.FIELD_EXPRESSION, value, "recipient").formPropertiesExtension!![0].expression.shouldBeEqualTo(value)} ("EXPR0=EXPR1");
        {value: String -> readAndUpdate(PropertyType.FIELD_NAME, value, "multiline").formPropertiesExtension!![1].name.shouldBeEqualTo(value)} ("new mulitline name");
    }

    @Test
    fun `Service task nested elements are emptyable`() {
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_NAME, "recipient", "activiti:field name=\"recipient\"", "<activiti:field>", "<activiti:field/>", "activiti:expression")
            .formPropertiesExtension?.map { it.name }?.shouldNotContain("recipient")
        readAndSetNullStringAndAssertItIsRemoved(PropertyType.FIELD_EXPRESSION, "recipient", "activiti:expression").formPropertiesExtension!![0].expression.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullStringAndAssertItIsRemoved(property: PropertyType, propertyIndex: String, vararg shouldNotContainStr: String): BpmnUserTask {
        val event = StringValueUpdatedEvent(elementId, property, "", propertyIndex = listOf(propertyIndex))
        val updated = updateBpmnFile(parser, FILE, listOf(event))
        shouldNotContainStr.forEach { updated.shouldNotContain(it) }
        return readUserTaskWithExtensions(parser.parse(updated))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnUserTask {
        return readUserTaskWithExtensions(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = listOf(propertyIndex))))
    }

    private fun readUserTaskWithExtensions(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.shouldHaveSingleItem()
    }
}
