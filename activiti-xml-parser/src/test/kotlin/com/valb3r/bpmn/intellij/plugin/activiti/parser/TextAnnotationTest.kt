package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class TextAnnotationTest {

    @Test
    fun `text annotations are parsed exposed and updated`() {
        val parser = ActivitiParser()
        val parsed = parser.parse("text-annotation.bpmn20.xml".asResource()!!)
        val annotation = parsed.process.body!!.textAnnotation!!.single()

        annotation.text!!.text.shouldBeEqualTo("Text annotation")
        annotation.textFormat.shouldBeEqualTo("text/plain")
        parsed.process.body!!.association!!.single().targetRef.shouldBeEqualTo("textAnnotationId")
        parsed.propsOf("textAnnotationId")[PropertyType.TEXT_ANNOTATION_TEXT]
            .shouldBeEqualTo(Property("Text annotation"))

        val updated = readAndUpdateProcess(
            parser,
            "text-annotation.bpmn20.xml",
            StringValueUpdatedEvent(BpmnElementId("textAnnotationId"), PropertyType.TEXT_ANNOTATION_TEXT, "Updated annotation text"),
        )

        updated.process.body!!.textAnnotation!!.single().text!!.text.shouldBeEqualTo("Updated annotation text")
    }
}
