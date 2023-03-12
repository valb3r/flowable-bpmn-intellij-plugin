package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSingleItem
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

        val props = BpmnFileObject(processObject.processes, processObject.diagram).toView(ActivitiObjectFactory()).processes[0].processElemPropertiesByElementId[task.id]!!
        props[PropertyType.ID]!!.value.shouldBeEqualTo(task.id.id)
        props.getAll(PropertyType.FIELD_NAME)[0].value.shouldBeNull()
        props.getAll(PropertyType.FIELD_EXPRESSION)[0].value.shouldBeNull()
        props.getAll(PropertyType.FIELD_STRING)[0].value.shouldBeNull()
    }

    private fun readServiceTask(processObject: BpmnFileObject): BpmnServiceTask {
        return processObject.processes[0].body!!.serviceTask!!.shouldHaveSingleItem()
    }
}
