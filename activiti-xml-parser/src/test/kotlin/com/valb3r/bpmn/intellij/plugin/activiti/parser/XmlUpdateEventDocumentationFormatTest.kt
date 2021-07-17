package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class XmlUpdateEventDocumentationFormatTest {

    private val startEventId = "startevent1";

    private val parser = ActivitiParser()

    @Test
    fun `Removing element does not break 'documentation' element formatting`() {
        val originalProcess = readProcess()
        val updatedProcess = readAndUpdateProcess(BpmnElementRemovedEvent(BpmnElementId(startEventId)))
        updatedProcess.process.body!!.scriptTask!![0].documentation
            .shouldBeEqualTo(originalProcess.process.body!!.scriptTask!![0].documentation)
    }

    private fun readProcess(): BpmnProcessObject {
        val process = parser.parse("documentation-element-formatting.bpmn20.xml".asResource()!!)
        process.shouldNotBeNull()
        return process
    }

    private fun readAndUpdateProcess(event: EventPropagatableToXml): BpmnProcessObject {
        val updated = parser.update(
            "documentation-element-formatting.bpmn20.xml".asResource()!!,
            listOf(event)
        )

        updated.shouldNotBeNull()

        return parser.parse(updated)
    }
}