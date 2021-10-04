package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.readAndUpdateProcess
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnMuleTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.ValueInArray
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/empty-service-task.bpmn20.xml"

internal class ActivityServiceTaskEmptyUpdateWithNestedExtensionTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("emptyServiceTaskId")

    @Test
    fun `Service task with nested extensions is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        val task = readServiceTask(processObject)
        task.id.shouldBeEqualTo(elementId)
        task.name.shouldBeEqualTo("Service task with extension")
        task.documentation.shouldBeNull()
        task.failedJobRetryTimeCycle?.shouldBeEqualTo("R10/PT5M")

        val props = BpmnProcessObject(processObject.process, processObject.diagram).toView(ActivitiObjectFactory()).elemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props.getAll(PropertyType.FIELD_NAME)[0].grpVal().value.shouldBeNull()
        props.getAll(PropertyType.FIELD_EXPRESSION)[0].grpVal().value.shouldBeNull()
        props.getAll(PropertyType.FIELD_STRING)[0].value.shouldBeNull()
    }

    private fun readServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
    }

    private fun Property.grpVal(): ValueInArray {
        return this.value as ValueInArray
    }
}
