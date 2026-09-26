package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnTextAnnotation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

internal class TextAnnotationTest {

    @Test
    fun `new text annotation does not expose mail task text property`() {
        val factory = FlowableObjectFactory()
        val annotation = factory.newBpmnObject(BpmnTextAnnotation::class)

        factory.propertiesOf(annotation)[PropertyType.TEXT].shouldBeNull()
        factory.propertiesOf(annotation)[PropertyType.TEXT_ANNOTATION_TEXT].shouldBeEqualTo(Property(""))
    }

    @Test
    fun `text annotations are parsed exposed and updated`() {
        val parser = FlowableParser()
        val parsed = parser.parse("popurri.bpmn20.xml".asResource()!!)
        val annotation = parsed.process.body!!.textAnnotation!!.single { it.id.id == "globalAnnotationId" }

        annotation.text!!.text.shouldBeEqualTo("Global annotation text")
        parsed.propsOf("globalAnnotationId")[PropertyType.TEXT_ANNOTATION_TEXT]
            .shouldBeEqualTo(Property("Global annotation text"))

        val updated = readAndUpdateProcess(
            parser,
            "popurri.bpmn20.xml",
            StringValueUpdatedEvent(
                BpmnElementId("globalAnnotationId"),
                PropertyType.TEXT_ANNOTATION_TEXT,
                "Updated annotation text",
            ),
        )

        updated.process.body!!.textAnnotation!!.single { it.id.id == "globalAnnotationId" }.text!!.text
            .shouldBeEqualTo("Updated annotation text")
    }
}
