package com.valb3r.bpmn.intellij.plugin.camunda.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-323-no-attached-to-ref.bpmn"

class BoundaryEventsTest {

    private val parser = CamundaParser()
    private val boundaryEvent = BpmnElementId("boundaryEvent")

    @Test
    fun `attachedToRef is applied`() {
        val updated = parser.update(FILE.asResource()!!, listOf(StringValueUpdatedEvent(boundaryEvent, PropertyType.ATTACHED_TO_REF, "serviceTask")))

        val processObject = parser.parse(updated)

        processObject.process.body!!.boundaryErrorEvent!![0].attachedToRef?.id.shouldBeEqualTo("serviceTask")
    }
}