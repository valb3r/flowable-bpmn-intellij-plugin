package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnManualTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/manual-task.bpmn20.xml"

internal class ActivityManualTaskTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("manualTaskId")

    @Test
    fun `Manual task is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readManualTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Manual task name")
        task.documentation.shouldBeEqualTo("Docs for manual task")
        task.async!!.shouldBeTrue()
        // TODO 'exclusive' ?
        task.isForCompensation!!.shouldBeTrue()

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.ASYNC]!!.value.shouldBeEqualTo(task.async)
        props[PropertyType.IS_FOR_COMPENSATION]!!.value.shouldBeEqualTo(task.isForCompensation)
    }

    @Test
    fun `Manual task is updatable`() {
        {value: String -> readAndUpdate(PropertyType.ID, value).id.id.shouldBeEqualTo(value)} ("new Id");
        {value: String -> readAndUpdate(PropertyType.NAME, value).name.shouldBeEqualTo(value)} ("new Name");
        {value: String -> readAndUpdate(PropertyType.DOCUMENTATION, value).documentation.shouldBeEqualTo(value)} ("new docs");
        {value: Boolean -> readAndUpdate(PropertyType.ASYNC, value).async.shouldBeEqualTo(value)} (false);
        {value: Boolean -> readAndUpdate(PropertyType.IS_FOR_COMPENSATION, value).isForCompensation.shouldBeEqualTo(value)} (false)
    }

    @Test
    fun `Manual task fields are emptyable`() {
        readAndSetNullString(PropertyType.NAME).name.shouldBeNullOrEmpty()
        readAndSetNullString(PropertyType.DOCUMENTATION).documentation.shouldBeNullOrEmpty()
    }

    private fun readAndSetNullString(property: PropertyType): BpmnManualTask {
        return readManualTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, "")))
    }
    
    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnManualTask {
        return readManualTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readAndUpdate(property: PropertyType, newValue: Boolean): BpmnManualTask {
        return readManualTask(readAndUpdateProcess(parser, FILE, BooleanValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readManualTask(processObject: BpmnProcessObject): BpmnManualTask {
        return processObject.process.body!!.manualTask!!.shouldHaveSingleItem()
    }
}