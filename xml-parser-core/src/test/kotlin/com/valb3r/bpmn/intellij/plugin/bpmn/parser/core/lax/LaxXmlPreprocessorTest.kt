package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LaxXmlPreprocessorTest {

    @Test
    fun `incomplete start tag rule skips comments cdata and processing instructions`() {
        val input = """<!-- <ignoredComment -->
            <![CDATA[<ignoredCdata]]>
            <?sample <ignoredInstruction?>
            <root>
              <serviceT
            </root>""".trimIndent()

        val hunks = IncompleteStartTagNameRule().findHunks(input)

        assertEquals(1, hunks.size)
        assertEquals(input.indexOf("<serviceT"), hunks.single().first)
        assertEquals(input.indexOf("<serviceT") + "<serviceT".length - 1, hunks.single().last)
    }

    @Test
    fun `preprocessor records coordinates masks for parse and restores at update marker`() {
        val input = "<root>\n  <serviceT\n</root>"
        val preprocessor = LaxXmlPreprocessor()

        val prepared = preprocessor.prepare(input)
        val hunk = prepared.hunks.single()

        assertEquals(1, hunk.lineStart)
        assertEquals(2, hunk.charStart)
        assertEquals(1, hunk.lineEnd)
        assertEquals(11, hunk.charEnd)
        assertEquals("<serviceT", hunk.text)
        assertTrue(prepared.xml.substring(hunk.startOffset, hunk.endOffset).isBlank())

        val update = preprocessor.prepareForUpdate(input, prepared.hunks)
        assertEquals("<serviceT", preprocessor.restore(update.xml, update.markers).substring(hunk.startOffset, hunk.endOffset))
    }
}
