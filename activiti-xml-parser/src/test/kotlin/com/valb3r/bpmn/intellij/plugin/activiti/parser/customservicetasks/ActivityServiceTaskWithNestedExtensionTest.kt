package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnMuleTask
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

    @Test
    fun `Service task with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readServiceTask(processObject)
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
        {value: String -> readAndUpdate(PropertyType.ID, value, "recipient").id.id.shouldBeEqualTo(value)} ("new Id")
    }

    @Test
    fun `Service task nested elements are emptyable`() {
//        readAndSetNullString(PropertyType.FIELD_NAME, "recipient").name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.FIELD_STRING, "multiline").name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.FIELD_EXPRESSION, "recipient").name.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType, propertyIndex: String): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "", propertyIndex = propertyIndex)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String, propertyIndex: String): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue, propertyIndex = propertyIndex)))
    }

    private fun readServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
    }

    private fun Property.grpVal(): ValueInArray {
        return this.value as ValueInArray
    }
}
