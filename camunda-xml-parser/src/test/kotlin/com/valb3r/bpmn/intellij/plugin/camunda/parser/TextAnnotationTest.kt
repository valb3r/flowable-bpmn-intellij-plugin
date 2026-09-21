package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class TextAnnotationTest {

    @Test
    fun `text annotations are parsed exposed and updated`() {
        val parser = CamundaParser()
        val parsed = parser.parse("popurri.bpmn".asResource()!!)
        val annotation = parsed.process.body!!.textAnnotation!!.single { it.id.id == "textAnnotationId" }

        annotation.text!!.text.shouldBeEqualTo("Text annotation")
        annotation.textFormat.shouldBeEqualTo("text/plain")
        parsed.propsOf("textAnnotationId")[PropertyType.TEXT_ANNOTATION_TEXT]
            .shouldBeEqualTo(Property("Text annotation"))

        val updated = readAndUpdateProcess(
            parser,
            "popurri.bpmn",
            StringValueUpdatedEvent(
                BpmnElementId("textAnnotationId"),
                PropertyType.TEXT_ANNOTATION_TEXT,
                "Updated annotation text",
            ),
        )

        updated.process.body!!.textAnnotation!!.single { it.id.id == "textAnnotationId" }.text!!.text
            .shouldBeEqualTo("Updated annotation text")
    }
}
