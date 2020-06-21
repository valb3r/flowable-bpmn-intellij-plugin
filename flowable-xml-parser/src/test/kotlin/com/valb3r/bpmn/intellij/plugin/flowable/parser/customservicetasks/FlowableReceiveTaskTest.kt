package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnReceiveTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/receive-task.bpmn20.xml"

internal class FlowableReceiveTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("receiveTaskId")

    @Test
    fun `Receive task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readReceiveTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Receive task name")
        task.documentation.shouldBeEqualTo("Docs for receive task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
    }

    @Test
    fun `Receive task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnReceiveTask {
        return readReceiveTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnReceiveTask {
        return readReceiveTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readReceiveTask(processObject: BpmnProcessObject): BpmnReceiveTask {
        val task = processObject.process.body!!.receiveTask!!.shouldHaveSingleItem()
        return task
    }
}