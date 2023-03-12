package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnExternalTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/external-task.bpmn20.xml"

class FlowableExternalTaskTest {
    private val parser = FlowableParser()
    private val elementId = BpmnElementId("externalId")

    @Test
    fun `External task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readExternalTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("NameExternal")
        task.documentation.shouldBeEqualTo("Docs for external task")
        task.jobTopic.shouldBeEqualTo("topic")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(FlowableObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.JOB_TOPIC]!!.value.shouldBeEqualTo(task.jobTopic)
    }

    @Test
    fun `External task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: String -> readAndUpdate(PropertyType.JOB_TOPIC, value).jobTopic.shouldBeEqualTo(value)} ("new Topic");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
    }

    @Test
    fun `External task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.JOB_TOPIC).jobTopic.shouldBeNullOrEmpty()
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
