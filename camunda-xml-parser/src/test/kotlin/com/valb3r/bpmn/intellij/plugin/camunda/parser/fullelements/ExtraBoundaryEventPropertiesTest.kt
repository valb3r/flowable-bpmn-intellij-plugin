package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

private const val FILE = "fullelements/full-boundary-events-and-timer.bpmn"

internal class ExtraBoundaryEventPropertiesTest {

    private val parser = CamundaParser()

    @Test
    fun `timerStartEvent extra essential properties are parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)
        val event = processObject.process.body!!.timerStartEvent!!.find {it.id.id == "timerStartDateEvent"}!!
        println()
    }

    @Test
    fun `timerStartEvent extra essential properties are updatable`() {
        val processObject = parser.parse(FILE.asResource()!!)
        val event = processObject.process.body!!.timerStartEvent!!.shouldHaveSize(1)[0]

    }


    @Test
    fun `timerIntermediateEvent extra essential properties`() {

    }

    @Test
    fun `MessageBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `EscalationBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `ErrorBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `SignalBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `TimerBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `ConditionalBoundaryEvent extra essential properties`() {

    }
}