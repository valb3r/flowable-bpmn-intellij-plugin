package com.valb3r.bpmn.intellij.plugin.flowable.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task-with-extension.bpmn20.xml"

internal class FlowableServiceTaskWithExtensionElementsTest {

    private val parser = FlowableParser()
    private val elementId = BpmnElementId("serviceTaskWithExtensionId")

    @Test
    fun `Service task with failedJobRetryTimeCycle is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)
        val task = processObject.processes[0].body!!.serviceTask!![0]

        task.id.shouldBeEqualTo(elementId)
        task.failedJobRetryTimeCycle.shouldBeEqualTo("R10/PT5M")

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(FlowableObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
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
        return processObject.processes[0].body!!.serviceTask!!.shouldHaveSingleItem()
    }
}
