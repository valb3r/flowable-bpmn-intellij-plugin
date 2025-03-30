package com.valb3r.bpmn.intellij.plugin.camunda.parser.multi_instance

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
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
import org.junit.jupiter.api.Test

private const val FILE = "multi-instance/multi-instance-service-task.bpmn"

class CamundaMultiInstanceTest {
    private val parser = CamundaParser()
    private val sequentialTaskId = BpmnElementId("sequentialTask")
    private val parallelTaskId = BpmnElementId("sequentialTask")

    @Test
    fun `Multi instance sequential service task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSequentialServiceTask(processObject)
        task.id.shouldBeEqualTo(sequentialTaskId)
        task.name.shouldBeEqualTo("NameExternal")
        task.documentation.shouldBeEqualTo("Docs for external task")
        task.asyncBefore!!.shouldBeTrue()
        task.asyncAfter!!.shouldBeTrue()
        // TODO 'exclusive' ?
//        CamundaObjectFactory
        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC_BEFORE]!!.value.shouldBeEqualTo(task.asyncBefore)
        props[PropertyType.ASYNC_AFTER]!!.value.shouldBeEqualTo(task.asyncAfter)
    }

    @Test
    fun `Multi instance parameters are updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC_AFTER, value).asyncAfter.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC_BEFORE, value).asyncBefore.shouldBeEqualTo(value)} (false);
    }

    @Test
    fun `Multi instance parameters are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
    }

    // Change task type
    // Remove multi-instance

    private fun readAndSetNullString(property: PropertyType): BpmnServiceTask {
        return readSequentialServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(sequentialTaskId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnServiceTask {
        return readSequentialServiceTask(
            readAndUpdateProcess(
                parser,
                FILE,
                StringValueUpdatedEvent(sequentialTaskId, property, newValue)
            )
        )
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnServiceTask {
        return readSequentialServiceTask(
            readAndUpdateProcess(
                parser,
                FILE,
                BooleanValueUpdatedEvent(sequentialTaskId, property, newValue)
            )
        )
    }

    private fun readSequentialServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.filter { it.id == sequentialTaskId }[0]
    }
}