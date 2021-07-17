package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BpmnShapeObjectAddedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class XmlUpdateEventDocumentationFormatTest {

    private val startEventId = "startevent1"
    private val parentProcess = BpmnElementId("m40191.SBWS-CallActivity-PdfGenerator-MapParameter")
    private val documentationProcessName = "documentation-element-formatting.bpmn20.xml"

    private val parser = FlowableParser()

    @Test
    fun `Parsing respects newline formatting`() {
        val originalProcess = documentationProcessName.asResource()!!
        val updated = parser.update(documentationProcessName.asResource()!!, listOf())
        updated.count { it == '\n' }.shouldBeEqualTo(originalProcess.count { it == '\n' } + 1)
    }

    @Test
    fun `New element is added with new line`() {
        val originalProcess = documentationProcessName.asResource()!!
        val testId = BpmnElementId("test")
        val updated = parser.update(
            documentationProcessName.asResource()!!, listOf(
                BpmnShapeObjectAddedEvent(
                    WithParentId(
                        parentProcess,
                        BpmnServiceTask(
                            testId,
                            "test",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                    ),
                    ShapeElement(DiagramElementId("id"), testId, BoundsElement(0.0f, 0.0f, 0.0f, 0.0f)),
                    emptyMap()
                )
            )
        )

        updated.count { it == '\n' }.shouldBeEqualTo(originalProcess.count { it == '\n' } + 5) // Shape + Diagram \n
    }

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