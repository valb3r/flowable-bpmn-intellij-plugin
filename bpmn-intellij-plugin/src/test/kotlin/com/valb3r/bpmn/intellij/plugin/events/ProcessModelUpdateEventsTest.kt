package com.valb3r.bpmn.intellij.plugin.events


import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

internal class ProcessModelUpdateEventsTest {

    @Test
    fun `File state matches for CRLF and LF line endings`() {
        val stateLf = "test\ntest\ndata"
        val stateCrLf = stateLf.replace("\n", "\r\n")

        val updateEvents = ProcessModelUpdateEvents(mock(FileCommitter::class.java), mutableListOf())
        updateEvents.reset(stateLf)
        updateEvents.fileStateMatches(stateCrLf).shouldBeTrue()
    }
}
