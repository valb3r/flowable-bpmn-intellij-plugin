package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyGroupEntry
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyGroupEntryType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task-with-nested-extensions.bpmn20.xml"

internal class ActivitiServiceTaskWithNestedExtensionTest {

    private val parser = ActivitiParser()
    private val elementId = BpmnElementId("serviceTaskWithExtensionId")

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
        props[PropertyType.NAME]!!.value.shouldBeEqualTo(task.name)
        props[PropertyType.DOCUMENTATION]!!.value.shouldBeEqualTo(task.documentation)
        props[PropertyType.FAILED_JOB_RETRY_CYCLE]!!.value.shouldBeEqualTo(task.failedJobRetryTimeCycle)
//        val entries = (props[PropertyType.GENERIC_FIELDS]!!.value as List<PropertyGroupEntry>).shouldHaveSize(6)
//
//        val recipient = fieldByIndex(entries, 0)
//        recipient[PropertyGroupEntryType.FIELD_NAME]!!.value.shouldBeEqualTo("recipient")
//        recipient[PropertyGroupEntryType.FIELD_EXPRESSION]!!.value.shouldBeEqualTo("userId:\${accountId}")
//
//        val multiline = fieldByIndex(entries, 1)
//        multiline[PropertyGroupEntryType.FIELD_NAME]!!.value.shouldBeEqualTo("multiline")
//        multiline[PropertyGroupEntryType.FIELD_STRING]!!.value.shouldBeEqualTo("This\n" +
//                "                is\n" +
//                "                multiline\n" +
//                "                text\n" +
//                "                ")
    }

    private fun fieldByIndex(entries: List<PropertyGroupEntry>, index: Int) =
        entries.filter { it.index == index }.associateBy({ it.type }, { it.value as Property? })

    private fun readServiceTask(processObject: BpmnProcessObject): BpmnServiceTask {
        return processObject.process.body!!.serviceTask!!.shouldHaveSingleItem()
    }
}
