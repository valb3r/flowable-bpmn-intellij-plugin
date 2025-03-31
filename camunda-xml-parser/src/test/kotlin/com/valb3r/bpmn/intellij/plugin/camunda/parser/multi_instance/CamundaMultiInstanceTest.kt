package com.valb3r.bpmn.intellij.plugin.camunda.parser.multi_instance

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.ServiceTask
import com.valb3r.bpmn.intellij.plugin.camunda.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

private const val FILE = "multi-instance/multi-instance-service-task.bpmn"

class CamundaMultiInstanceTest {
    private val parser = CamundaParser()
    private val plainServiceTask = BpmnElementId("plainServiceTask")
    private val sequentialTaskId = BpmnElementId("sequentialTask")
    private val sequentialUserTaskId = BpmnElementId("sequentialUserTask")
    private val sequentialCallActivityTaskId = BpmnElementId("sequentialCallActivity")
    private val parallelTaskId = BpmnElementId("parallelTask")

    @Test
    fun `Plain service task is correctly parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readPlainServiceTask(processObject)
        task.id.shouldBeEqualTo(plainServiceTask)
        task.name.shouldBeEqualTo("plainServiceTask")
        task.multiInstanceLoopCharacteristics.shouldBeNull()
    }

    @Test
    fun `Multi instance parallel service task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readParallelServiceTask(processObject)
        task.id.shouldBeEqualTo(parallelTaskId)
        task.name.shouldBeEqualTo("parallelTask")
        task.multiInstanceLoopCharacteristics.shouldNotBeNull()
        task.multiInstanceLoopCharacteristics!!.isSequential!!.shouldBeFalse()
        task.multiInstanceLoopCharacteristics!!.collection!!.shouldBeEqualTo("multiInstanceColl")
        task.multiInstanceLoopCharacteristics!!.elementVariable!!.shouldBeEqualTo("elementVar")
        task.multiInstanceLoopCharacteristics!!.loopCardinality!!.type.shouldBeEqualTo("bpmn:tFormalExpression")
        task.multiInstanceLoopCharacteristics!!.loopCardinality!!.expression.shouldBeEqualTo("1")
        task.multiInstanceLoopCharacteristics!!.completionCondition!!.type.shouldBeEqualTo("bpmn:tFormalExpression")
        task.multiInstanceLoopCharacteristics!!.completionCondition!!.expression.shouldBeEqualTo("if (true)")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.MULTI_INSTANCE_LOOP_IS_SEQUENTIAL]!!.value.shouldBeEqualTo(false)
        props[PropertyType.MULTI_INSTANCE_LOOP_COLLECTION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.collection)
        props[PropertyType.MULTI_INSTANCE_LOOP_ELEMENT_VARIABLE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.elementVariable)
        props[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY_TYPE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.loopCardinality!!.type)
        props[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY_EXPRESSION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.loopCardinality!!.expression)
        props[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION_TYPE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.completionCondition!!.type)
        props[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION_EXPRESSION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.completionCondition!!.expression)
    }

    @Test
    fun `Multi instance sequential service task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSequentialServiceTask(processObject)
        task.id.shouldBeEqualTo(sequentialTaskId)
        task.name.shouldBeEqualTo("sequentialTask")
        task.multiInstanceLoopCharacteristics.shouldNotBeNull()
        task.multiInstanceLoopCharacteristics!!.isSequential!!.shouldBeEqualTo(true)
        task.multiInstanceLoopCharacteristics!!.collection!!.shouldBeEqualTo("multiInstanceColl")
        task.multiInstanceLoopCharacteristics!!.elementVariable!!.shouldBeEqualTo("elementVar")
        task.multiInstanceLoopCharacteristics!!.loopCardinality!!.type.shouldBeEqualTo("bpmn:tFormalExpression")
        task.multiInstanceLoopCharacteristics!!.loopCardinality!!.expression.shouldBeEqualTo("1")
        task.multiInstanceLoopCharacteristics!!.completionCondition!!.type.shouldBeEqualTo("bpmn:tFormalExpression")
        task.multiInstanceLoopCharacteristics!!.completionCondition!!.expression.shouldBeEqualTo("if (true)")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(CamundaObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.MULTI_INSTANCE_LOOP_IS_SEQUENTIAL]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.isSequential)
        props[PropertyType.MULTI_INSTANCE_LOOP_COLLECTION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.collection)
        props[PropertyType.MULTI_INSTANCE_LOOP_ELEMENT_VARIABLE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.elementVariable)
        props[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY_TYPE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.loopCardinality!!.type)
        props[PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY_EXPRESSION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.loopCardinality!!.expression)
        props[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION_TYPE]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.completionCondition!!.type)
        props[PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION_EXPRESSION]!!.value.shouldBeEqualTo(task.multiInstanceLoopCharacteristics!!.completionCondition!!.expression)
    }

    @Test
    fun `Multi instance sequential user task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readSequentialUserTask(processObject)
        task.id.shouldBeEqualTo(sequentialUserTaskId)
        task.multiInstanceLoopCharacteristics.shouldNotBeNull()
        task.multiInstanceLoopCharacteristics!!.isSequential!!.shouldBeTrue()
    }

    @Test
    fun `Multi instance sequential call activity is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val activity = readSequentialCallActivityTask(processObject)
        activity.id.shouldBeEqualTo(sequentialCallActivityTaskId)
        activity.multiInstanceLoopCharacteristics.shouldNotBeNull()
        activity.multiInstanceLoopCharacteristics!!.isSequential!!.shouldBeTrue()
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

    private fun readParallelServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.filter { it.id == parallelTaskId }[0]
    }

    private fun readSequentialUserTask(processObject: BpmnProcessObject): BpmnUserTask {
        return processObject.process.body!!.userTask!!.filter { it.id == sequentialUserTaskId }[0]
    }

    private fun readSequentialCallActivityTask(processObject: BpmnProcessObject): BpmnCallActivity {
        return processObject.process.body!!.callActivity!!.filter { it.id == sequentialCallActivityTaskId }[0]
    }

    private fun readPlainServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.filter { it.id == plainServiceTask }[0]
    }
}