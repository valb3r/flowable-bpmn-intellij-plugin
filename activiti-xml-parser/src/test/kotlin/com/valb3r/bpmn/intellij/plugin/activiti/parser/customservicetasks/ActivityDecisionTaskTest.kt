package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnDecisionTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/decision-task.bpmn20.xml"

internal class ActivityDecisionTaskTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("decisionTaskId")

    @Test
    fun `Decision task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readDecisionTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Decision task name")
        task.documentation.shouldBeEqualTo("Docs for decision task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()
        task.decisionTableReferenceKey.shouldBeEqualTo("Tablekey")
        task.decisionTaskThrowErrorOnNoHits.shouldBeNull() // Not supported by activity
        task.fallbackToDefaultTenantCdata.shouldBeNull() // Not supported by activity

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(ActivitiObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
        props[PropertyType.DECISION_TABLE_REFERENCE_KEY]!!.value.shouldBeEqualTo(task.decisionTableReferenceKey)
        props[PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS].shouldBeNull()
        props[PropertyType.FALLBACK_TO_DEF_TENANT_CDATA].shouldBeNull()
    }

    @Test
    fun `Decision task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false);
        {value: String -> readAndUpdate(PropertyType.DECISION_TABLE_REFERENCE_KEY, value).decisionTableReferenceKey.shouldBeEqualTo(value)} ("My table");
        {value: Boolean -> readAndUpdate(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS, value).decisionTaskThrowErrorOnNoHits.shouldBeNull()} (false);
        {value: Boolean -> readAndUpdate(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA, value).fallbackToDefaultTenantCdata.shouldBeNull()} (false);
    }

    @Test
    fun `Decision task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DECISION_TABLE_REFERENCE_KEY).decisionTableReferenceKey.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnDecisionTask {
        return readDecisionTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnDecisionTask {
        return readDecisionTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnDecisionTask {
        return readDecisionTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readDecisionTask(processObject: BpmnFileObject): BpmnDecisionTask {
        return processObject.processes[0].body!!.decisionTask!!.shouldHaveSingleItem()
    }
}
