package com.valb3r.bpmn.intellij.plugin.camunda.parser

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class CamundaLaxParsingTest {

    @Test
    fun `Camunda parser exposes incomplete element name as BPMN lax hunk`() {
        val input = "popurri.bpmn".asResource()!!
            .replaceFirst("</bpmn:process>", "<serviceT</bpmn:process>")

        CamundaParser().parse(input).laxHunks.single().text.shouldBeEqualTo("<serviceT")
    }
}
