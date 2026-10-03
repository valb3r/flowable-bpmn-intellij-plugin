package com.valb3r.bpmn.intellij.plugin.activiti.parser

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class ActivitiLaxParsingTest {

    @Test
    fun `Activiti parser exposes incomplete element name as BPMN lax hunk`() {
        val input = "empty-process-name.bpmn20.xml".asResource()!!
            .replaceFirst("</process>", "<serviceT</process>")

        ActivitiParser().parse(input).laxHunks.single().text.shouldBeEqualTo("<serviceT")
    }
}
