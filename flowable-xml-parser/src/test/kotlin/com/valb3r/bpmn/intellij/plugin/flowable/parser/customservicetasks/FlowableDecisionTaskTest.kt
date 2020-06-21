package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnDecisionTask
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

private const val FILE = "custom-service-tasks/decision-task.bpmn20.xml"

internal class FlowableDecisionTaskTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("decisionTaskId")

    @Test
    fun `Camel task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readDecisionTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Decision task name")
        task.documentation.shouldBeEqualTo("Docs for decision task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.decisionTableReferenceKey.shouldBeEqualTo("Tablekey")
        task.decisionTaskThrowErrorOnNoHits.shouldBeEqualTo(true)
        task.fallbackToDefaultTenantCdata.shouldBeEqualTo(true)

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(FlowableObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.DECISION_TABLE_REFERENCE_KEY]!!.value.shouldBeEqualTo(task.decisionTableReferenceKey)
        props[PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS]!!.value.shouldBeEqualTo(task.decisionTaskThrowErrorOnNoHits)
        props[PropertyType.FALLBACK_TO_DEF_TENANT_CDATA]!!.value.shouldBeEqualTo(task.fallbackToDefaultTenantCdata)
    }

    @Test
    fun `Camel task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.DECISION_TABLE_REFERENCE_KEY, value).decisionTableReferenceKey.shouldBeEqualTo(value)} ("My table");
        {value: Boolean -> readAndUpdate(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS, value).decisionTaskThrowErrorOnNoHits.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA, value).fallbackToDefaultTenantCdata.shouldBeEqualTo(value)} (false);
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnDecisionTask {
        return readDecisionTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnDecisionTask {
        return readDecisionTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readDecisionTask(processObject: BpmnProcessObject): BpmnDecisionTask {
        val task = processObject.process.body!!.decisionTask!!.shouldHaveSingleItem()
        return task
    }
}