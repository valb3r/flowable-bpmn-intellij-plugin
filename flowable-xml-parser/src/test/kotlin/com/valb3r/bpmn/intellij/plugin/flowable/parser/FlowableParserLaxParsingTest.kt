package com.valb3r.bpmn.intellij.plugin.flowable.parser

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlowableParserLaxParsingTest {

    @Test
    fun `default parser accepts lax block carries it through BPMN view and restores after update`() {
        val parser = FlowableParser()
        val input = "empty-process-name.bpmn20.xml".asResource()!!
            .replaceFirst("</process>", "<serviceT</process>")

        val parsed = parser.parse(input)
        val hunk = parsed.laxHunks.single()

        hunk.text.shouldBeEqualTo("<serviceT")
        parsed.toView(FlowableObjectFactory()).laxHunks.shouldBeEqualTo(parsed.laxHunks)

        val updated = parser.update(input, emptyList(), parsed.laxHunks)
        assertTrue(updated.contains("<serviceT"))
        parser.parse(updated).laxHunks.single().text.shouldBeEqualTo("<serviceT")
    }

    @Test
    fun `lax parsing can be disabled and enabled dynamically`() {
        val input = "empty-process-name.bpmn20.xml".asResource()!!
            .replaceFirst("</process>", "<serviceT</process>")
        var laxParsingEnabled = false
        val parser = FlowableParser { laxParsingEnabled }

        assertThrows(Exception::class.java) { parser.parse(input) }

        laxParsingEnabled = true
        parser.parse(input).laxHunks.single().text.shouldBeEqualTo("<serviceT")
    }
}
