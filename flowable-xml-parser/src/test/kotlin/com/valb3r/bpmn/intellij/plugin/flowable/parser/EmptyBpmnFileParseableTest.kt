package com.valb3r.bpmn.intellij.plugin.flowable.parser

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

internal class EmptyBpmnFileParseableTest {

    private val parser = FlowableParser()

    @Test
    fun `blank BPMN file is parseable`() {
        assertEmptyProcess("")
    }

    @Test
    fun `definitions without a process are parseable`() {
        assertEmptyProcess("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"/>")
    }

    private fun assertEmptyProcess(input: String) {
        parser.validateForErrors(input).shouldBeNull()

        val processObject = parser.parse(input)
        processObject.process.id.id.shouldBeEqualTo("")
        processObject.process.body.shouldBeNull()
        processObject.diagram.shouldBeEmpty()
    }
}
