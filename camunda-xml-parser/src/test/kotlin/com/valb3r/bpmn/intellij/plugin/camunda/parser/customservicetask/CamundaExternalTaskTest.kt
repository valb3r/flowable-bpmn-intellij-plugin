package com.valb3r.bpmn.intellij.plugin.camunda.parser.customservicetask

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnExternalTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/external-task.bpmn20.xml"


class CamundaExternalTaskTest {
    private val parser = CamundaParser()
    private val elementId = BpmnElementId("externalId")

    @Test
    fun `External task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readExternalTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("NameExternal")
        task.documentation.shouldBeEqualTo("Docs for external task")
        task.jobTopic.shouldBeEqualTo("TestTopic")
        task.taskPriority.shouldBeEqualTo("12")
        task.asyncBefore!!.shouldBeTrue()
        task.asyncAfter!!.shouldBeTrue()
        // TODO 'exclusive' ?
//        CamundaObjectFactory
        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(CamundaObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.JOB_TOPIC]!!.value.shouldBeEqualTo(task.jobTopic)
        props[PropertyType.TASK_PRIORITY]!!.value.shouldBeEqualTo(task.taskPriority)
        props[PropertyType.ASYNC_BEFORE]!!.value.shouldBeEqualTo(task.asyncBefore)
        props[PropertyType.ASYNC_AFTER]!!.value.shouldBeEqualTo(task.asyncAfter)
    }

    @Test
    fun `External task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: String -> readAndUpdate(PropertyType.JOB_TOPIC, value).jobTopic.shouldBeEqualTo(value)} ("new Topic");
        {value: String -> readAndUpdate(PropertyType.TASK_PRIORITY, value).taskPriority.shouldBeEqualTo(value)} ("1");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC_AFTER, value).asyncAfter.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC_BEFORE, value).asyncBefore.shouldBeEqualTo(value)} (false);
    }

    @Test
    fun `External task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.JOB_TOPIC).jobTopic.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.TASK_PRIORITY).taskPriority.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnExternalTask {
        return readExternalTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnExternalTask {
        return readExternalTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnExternalTask {
        return readExternalTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readExternalTask(processObject: BpmnFileObject): BpmnExternalTask {
        return processObject.processes[0].body!!.externalTask!!.shouldHaveSingleItem()
    }
}
