package com.valb3r.bpmn.intellij.plugin.events

import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ProcessModelUpdateEventsTest {

    @Test
    fun `File state matches for CRLF and LF line endings`() {
        val stateLf = "test\ntest\ndata"
        val stateCrLf = stateLf.replace("\n", "\r\n")

        val updateEvents = ProcessModelUpdateEvents(mock(FileCommitter::class), mutableListOf())
        updateEvents.reset(stateLf)
        updateEvents.fileStateMatches(stateCrLf).shouldBeTrue()
    }
}
