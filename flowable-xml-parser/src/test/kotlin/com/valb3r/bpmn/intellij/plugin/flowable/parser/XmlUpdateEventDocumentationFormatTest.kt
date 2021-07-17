package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BpmnElementRemovedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class XmlUpdateEventDocumentationFormatTest {

    private val startEventId = "startevent1"
    private val documentationProcessName = "documentation-element-formatting.bpmn20.xml"

    private val parser = FlowableParser()

    @Test
    fun `Removing element does not break 'documentation' element formatting`() {
        val originalProcess = readProcess()
        val updatedProcess = readAndUpdateProcess(BpmnElementRemovedEvent(BpmnElementId(startEventId)))
        updatedProcess.process.body!!.scriptTask!![0].documentation
            .shouldBeEqualTo(originalProcess.process.body!!.scriptTask!![0].documentation)
    }

    private fun readProcess(): BpmnProcessObject {
        val process = parser.parse(documentationProcessName.asResource()!!)
        process.shouldNotBeNull()
        return process
    }

    private fun readAndUpdateProcess(event: EventPropagatableToXml): BpmnProcessObject {
        val updated = parser.update(
            documentationProcessName.asResource()!!,
            listOf(event)
        )

        updated.shouldNotBeNull()

        return parser.parse(updated)
    }
}