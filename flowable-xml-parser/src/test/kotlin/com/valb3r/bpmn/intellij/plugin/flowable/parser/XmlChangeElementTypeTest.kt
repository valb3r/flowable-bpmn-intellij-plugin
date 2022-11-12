package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.UserTask
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BpmnElementTypeChangeEvent
import org.amshove.kluent.shouldBeBlank
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test


internal class XmlChangeElementTypeTest {

    private val serviceTaskId = BpmnElementId("serviceTaskId")
    private val parentId = BpmnElementId("popurri")

    private val parser = FlowableParser()

    @Test
    fun `String value update event on flat element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(
            parser,
            "popurri.bpmn20.xml",
            BpmnElementTypeChangeEvent(
                serviceTaskId,
                BpmnUserTask(serviceTaskId),
                PropertyTable(mutableMapOf(
                    Pair(PropertyType.ID, mutableListOf(Property(serviceTaskId.id))),
                    Pair(PropertyType.NAME, mutableListOf(Property("Some name")))
                )),
                parentId
            )
        )

        updatedProcess.process.body!!.serviceTask?.filter { it.id == serviceTaskId }?.shouldBeEmpty()
        val userTask = updatedProcess.process.body!!.userTask!!.filter { it.id == serviceTaskId }.shouldHaveSingleItem()
        userTask.name.shouldBeEqualTo("Some name")
        userTask.documentation?.shouldBeBlank()
    }
}