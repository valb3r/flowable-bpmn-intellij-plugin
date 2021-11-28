package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task-with-extension.bpmn20.xml"

internal class ActivityServiceTaskWithExtensionElementsTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("serviceTaskWithExtensionId")

    @Test
    fun `Service task with failedJobRetryTimeCycle is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)
        val task = processObject.process.body!!.serviceTask!![0]

        task.id.shouldBeEqualTo(elementId)
        task.failedJobRetryTimeCycle.shouldBeEqualTo("R10/PT5M")

        val props = BpmnFileObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.FAILED_JOB_RETRY_CYCLE]!!.value.shouldBeEqualTo(task.failedJobRetryTimeCycle)
    }

    @Test
    fun `Service task failedJobRetryTimeCycle is updatable`() {
        {value: String -> readAndUpdate(PropertyType.FAILED_JOB_RETRY_CYCLE, value).failedJobRetryTimeCycle.shouldBeEqualTo(value)} ("PT999/12");
    }

    private fun readAndUpdate(property: PropertyType, newValue: String): BpmnServiceTask {
        return readServiceTask(readAndUpdateProcess(parser, FILE, StringValueUpdatedEvent(elementId, property, newValue)))
    }

    private fun readServiceTask(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
    }
}
